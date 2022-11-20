package com.android.settings.datausage;

import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.LoaderManager;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.android.settings.R;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.SummaryForAllUidLoader;
import com.android.settingslib.net.UidDetailProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class DataActivity extends Activity {
// https://github.com/q960757274/PhoneAssistant
    private static final String TAG = "Ylw_Data";
    private AppOpsManager mAppOps;
    private NetworkStatsManager mNetworkStatsManager;
    private TelephonyManager mTm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        mAppOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        mNetworkStatsManager = (NetworkStatsManager) getSystemService(Context.NETWORK_STATS_SERVICE);
        mTm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        hasPermissionToReadNetworkStats();
        new Thread(() -> test1()).start();

    }

    private boolean hasPermissionToReadNetworkStats() {
        final AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }
        // 打开“有权查看使用情况的应用”页面
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
        return false;
    }

    private Context getContext() {
        return this;
    }

//    private void test() {
//        String subId = mTm.getSubscriberId();
//
//        Log.i(TAG, "test subId=" + subId);
//
//        try {
//            long endTime = System.currentTimeMillis();
//            NetworkStats.Bucket bucket = mNetworkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
//                    subId,
//                    0,
//                    endTime);
//            Log.i(TAG, "test: total:" + bucket.getTxBytes() + ":" + bucket.getRxBytes());
//            Log.i(TAG,
//                    "test: total:" + getNetFileSizeDescription(bucket.getTxBytes()) + ":" + getNetFileSizeDescription(bucket.getRxBytes()));
//            NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
//            NetworkStats summaryStats = mNetworkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, subId, 0,
//                    endTime);
//            long sumTxBytes = 0;
//            long sumRxBytes = 0;
//            do {
//                summaryStats.getNextBucket(summaryBucket);
//                int summaryUid = summaryBucket.getUid();
//                long rxBytes = summaryBucket.getRxBytes();
//                long txBytes = summaryBucket.getTxBytes();
//                sumTxBytes += txBytes;
//                sumRxBytes += rxBytes;
//                Log.i(TAG,
//                        "test: uid=" + summaryUid + ",rxBytes=" + rxBytes + "-" + getNetFileSizeDescription(rxBytes) + ",txBytes=" + txBytes + "-" + getNetFileSizeDescription(txBytes));
//                getPkgNameByUid(summaryUid);
//            } while (summaryStats.hasNextBucket());
//
//            Log.i(TAG, "test: sum:" + sumTxBytes + ":" + getNetFileSizeDescription(sumTxBytes) + ":" + sumRxBytes +
//                    ":" + getNetFileSizeDescription(sumRxBytes));
//        } catch (Exception e) {
//            Log.e(TAG, "test: ", e);
//        }
//    }

    /**
     * 根据包名获取uid
     *
     * @param context     上下文
     * @param packageName 包名
     */
    private int getUidByPackageName(Context context, String packageName) {
        int uid = -1;
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            uid = packageInfo.applicationInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return uid;
    }

    private String getPkgNameByUid(int uid) {
        PackageManager packageManager = getPackageManager();
        String nameForUid = packageManager.getNameForUid(uid);
        String[] packagesForUid = packageManager.getPackagesForUid(uid);
        Log.d(TAG, "getPkgNameByUid: " + uid + ":" + nameForUid + ":" + Arrays.toString(packagesForUid));
        return nameForUid;
    }

    public String getNetFileSizeDescription(long size) {
        StringBuilder bytes = new StringBuilder();
        DecimalFormat format = new DecimalFormat("###.0");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        } else if (size < 1024) {
            if (size <= 0) {
                bytes.append("0B");
            } else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }

    private static final int LOADER_SUMMARY = 3;
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_START = "start";
    private static final String KEY_END = "end";
    private INetworkStatsSession mStatsSession;
    private NetworkPolicyManager mNetworkPolicyManager;
    public static final int POLICY_REJECT_METERED_BACKGROUND = 0x1;
    private UidDetailProvider mUidDetailProvider;

    private void test1() {
        mNetworkPolicyManager = NetworkPolicyManager.from(this);
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        mUidDetailProvider = new UidDetailProvider(this);

        List<SubscriptionInfo> infoList = subscriptionManager.getActiveSubscriptionInfoList();
        NetworkTemplate mobileAll =
                NetworkTemplate.buildTemplateMobileAll(mTm.getSubscriberId(infoList.get(0).getSubscriptionId()));
        NetworkTemplate template = NetworkTemplate.normalize(mobileAll, mTm.getMergedSubscriberIds());
        INetworkStatsService networkStatsService =
                INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));

        try {
            mStatsSession = networkStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        long end = System.currentTimeMillis();
        long start = end - 24 * 3600 * 1000L;
        final Bundle args = new Bundle();
        args.putParcelable(KEY_TEMPLATE, template);
        args.putLong(KEY_START, start);
        args.putLong(KEY_END, end);

        getLoaderManager().restartLoader(LOADER_SUMMARY, args, mSummaryCallbacks);
    }

    private final LoaderManager.LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderManager.LoaderCallbacks<
            NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(DataActivity.this, mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            final int[] restrictedUids = mNetworkPolicyManager.getUidsWithPolicy(POLICY_REJECT_METERED_BACKGROUND);
            bindStats(data, restrictedUids);
            updateEmptyVisible();
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            bindStats(null, new int[0]);
            updateEmptyVisible();
        }
    };

    /**
     * Bind the given {@link NetworkStats}, or {@code null} to clear list.
     */
    public void bindStats(NetworkStats stats, int[] restrictedUids) {
        ArrayList<AppItem> items = new ArrayList<>();
        long largest = 0;

        final int currentUserId = ActivityManager.getCurrentUser();
        UserManager userManager = UserManager.get(getContext());
        final List<UserHandle> profiles = userManager.getUserProfiles();
        final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

        NetworkStats.Entry entry = null;
        final int size = stats != null ? stats.size() : 0;
        Log.i(TAG, "bindStats: " + size);
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);
            Log.d(TAG, "bindStats entry: "+ entry);
            // Decide how to collapse items together
            final int uid = entry.uid;

            final int collapseKey;
            final int category;
            final int userId = UserHandle.getUserId(uid);
            if (UserHandle.isApp(uid)) {
                if (profiles.contains(new UserHandle(userId))) {
                    if (userId != currentUserId) {
                        // Add to a managed user item.
                        final int managedKey = UidDetailProvider.buildKeyForUser(userId);
                        largest = accumulate(managedKey, knownItems, entry, AppItem.CATEGORY_USER,
                                items, largest);
                    }
                    // Add to app item.
                    collapseKey = uid;
                    category = AppItem.CATEGORY_APP;
                } else {
                    // If it is a removed user add it to the removed users' key
                    final UserInfo info = userManager.getUserInfo(userId);
                    if (info == null) {
                        collapseKey = UID_REMOVED;
                        category = AppItem.CATEGORY_APP;
                    } else {
                        // Add to other user item.
                        collapseKey = UidDetailProvider.buildKeyForUser(userId);
                        category = AppItem.CATEGORY_USER;
                    }
                }
            } else if (uid == UID_REMOVED || uid == UID_TETHERING) {
                collapseKey = uid;
                category = AppItem.CATEGORY_APP;
            } else {
                collapseKey = android.os.Process.SYSTEM_UID;
                category = AppItem.CATEGORY_APP;
            }
            largest = accumulate(collapseKey, knownItems, entry, category, items, largest);
        }

        final int restrictedUidsMax = restrictedUids.length;
        for (int i = 0; i < restrictedUidsMax; ++i) {
            final int uid = restrictedUids[i];
            // Only splice in restricted state for current user or managed users
            if (!profiles.contains(new UserHandle(UserHandle.getUserId(uid)))) {
                continue;
            }

            AppItem item = knownItems.get(uid);
            if (item == null) {
                item = new AppItem(uid);
                item.total = -1;
                items.add(item);
                knownItems.put(item.key, item);
            }
            item.restricted = true;
        }

        Collections.sort(items);
        for (AppItem item : items) {
            Log.i(TAG, "bindStats AppItem: " + item+":-:"+uidString(item.uids));
        }

//        mApps.removeAll();
//        for (int i = 0; i < items.size(); i++) {
//            final int percentTotal = largest != 0 ? (int) (items.get(i).total * 100 / largest) : 0;
//            AppDataUsagePreference preference = new AppDataUsagePreference(getContext(),
//                    items.get(i), percentTotal, mUidDetailProvider);
//            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    AppDataUsagePreference pref = (AppDataUsagePreference) preference;
//                    AppItem item = pref.getItem();
//                    startAppDataUsage(item);
//                    return true;
//                }
//            });
//            mApps.addPreference(preference);
//        }
    }

   private String uidString(SparseBooleanArray uids){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uids.size(); i++) {
            int key = uids.keyAt(i);
            String pkgNameByUid = getPkgNameByUid(key);
            sb.append(",userid="+ key);
            sb.append("pkg="+ pkgNameByUid);
        }
        return sb.toString();
    }

    /**
     * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
     * Creates the item if needed.
     *
     * @param collapseKey  the collapse key used to map the item.
     * @param knownItems   collection of known (already existing) items.
     * @param entry        the network stats entry to extract data usage from.
     * @param itemCategory the item is categorized on the list view by this category. Must be
     */
    private static long accumulate(int collapseKey, final SparseArray<AppItem> knownItems,
                                   NetworkStats.Entry entry, int itemCategory, ArrayList<AppItem> items, long largest) {
        final int uid = entry.uid;
        AppItem item = knownItems.get(collapseKey);
        if (item == null) {
            item = new AppItem(collapseKey);
            item.category = itemCategory;
            items.add(item);
            knownItems.put(item.key, item);
        }
        item.addUid(uid);
        item.total += entry.rxBytes + entry.txBytes;
        return Math.max(largest, item.total);
    }

    private void updateEmptyVisible() {
//            if ((mApps.getPreferenceCount() != 0) !=
//                    (getPreferenceScreen().getPreferenceCount() != 0)) {
//                if (mApps.getPreferenceCount() != 0) {
//                    getPreferenceScreen().addPreference(mUsageAmount);
//                    getPreferenceScreen().addPreference(mApps);
//                } else {
//                    getPreferenceScreen().removeAll();
//                }
//            }
    }
}