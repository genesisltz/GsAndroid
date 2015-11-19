package me.genesis.gsandroid.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/***
 * 权限工具类<br>
 * 注意：一定要在Activity里重写onRequestPermissionsResult，并调用PermissionUtil.handleRequestPermissionsResult进行处理
 * <p/>
 * Created by genesisli on 2015/11/19.
 */
@SuppressWarnings("unused")
public class PermissionUtil {

    /**
     * 控制标志位：用于控制申请 WRITE_SETTING 权限的对话框是否可以弹出
     */
    private static boolean isRequestWriteSettingEnabled = false;

    /**
     * 禁止实例化工具类
     */
    private PermissionUtil() {
    }

    /**
     * 检查权限数组是否已经全部获取授权（不会发起权限请求）<br/>
     *
     * @param context 申请权限的Context对象
     * @param permissions 权限数组
     * @return true：传入的所有权限均已授权；<br>false：至少有一个权限未被授权
     */
    public static boolean checkSelfPermissions(Context context, String... permissions) {
        if (context == null) {
            LogUtil.e("permission", "调用 PermissionUtil.checkSelfPermissions 时，传入了空的context");
            return true;
        }
        if (permissions == null || permissions.length < 1) {
            return true;
        }
        for (String permission : permissions) {
            // 注意，这里有个坑：用ContextCompat的check方法，在API22及一下一直返回true
            // 只有用PermissionChecker才能正确的检测
            if (PermissionChecker.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                LogUtil.e("permission", "存在未获取的权限：" + getPermissionDescString(permission));
                return false;
            }
        }
        LogUtil.e("permission", "权限均已获取");
        return true;
    }

    /**
     * 首先检查权限，如果权限均已获得则返回true，否则自动发起权限请求并返回false<br>
     * 注意，如果传入的context不是Activity，则不会发起权限请求
     *
     * @param context 申请权限的Context对象
     * @param requestCode 请求码，将在Activity的onRequestPermissionsResult中带回来
     * @param permissions 要请求的权限（可以传入数组，或不定个数参数）
     */
    public static boolean checkOrRequestPermissions(Context context, int requestCode, String... permissions) {
        if (checkSelfPermissions(context, permissions)) {
            return true;
        } else if (context instanceof Activity) {
            // Activity才能发起权限申请，此处做一下转换
            Activity activity = (Activity) context;
            requestPermissions(activity, requestCode, permissions);
        }
        return false;
    }

    /**
     * 处理权限请求的返回结果，需要放在Activity的onRequestPermissionsResult中
     *
     * @param activity
     * @param requestCode
     * @param permissions
     * @param grantResults
     * @return true：申请的所有权限都获取到了；<br>false：有的权限被拒绝了
     */
    public static boolean handleRequestPermissionsResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            LogUtil.e("permission", "收到请求结果：" + getPermissionDescString(permissions[i]) + "，获取成功？：" + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
        }

        // 曾经处理过系统标准的权限请求之后，才可以开始弹出 WRITE_SETTING 的请求对话框
        isRequestWriteSettingEnabled = true;

        if (verifyPermissionResults(grantResults)) {
            // 申请的全部权限都获得了
            switch (requestCode) {
                // do something
            }
            Toast.makeText(activity, "权限获取成功\n请重新进入相应功能", Toast.LENGTH_SHORT).show();
            return true;
        }

        String msg = "";
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                msg += msg.equals("") ? "" : "，"; // 前面有文字的话，才加逗号
                msg += getPermissionDescString(permissions[i]);
            }
        }

        Toast.makeText(activity, "权限被拒绝\n可能导致应用功能异常\n请去设置界面中开启权限", Toast.LENGTH_SHORT).show();
        return false;
    }

    public static boolean checkWriteSettingPermission(Context context) {
        if (Build.VERSION.SDK_INT > 22) {
            if (Settings.System.canWrite(context)) {
                Log.e("genesis", "permission--->已获取WRITE_SETTINGS权限");
                return true;
            }
            return false;
        } else {
            // 6.0以下系统，直接返回true
            return true;
        }
    }

    public static void requestWriteSettingPermission(final Context context) {
        if (checkWriteSettingPermission(context)) {
            return;
        }
        if (isRequestWriteSettingEnabled) {
            new AlertDialogHolder(context).show();
        }
    }

    /**
     * 工具类：保证屏幕上只会存在一个这种对话框，且第二次显示时文字不一样
     */
    private static class AlertDialogHolder {

        private AlertDialog dialog;

        /**
         * 是否有任何一个AlertDialogHolder对话框，正在显示中
         */
        private static boolean isShowing = false;
        /**
         * 是否有任何一个AlertDialogHolder对话框，第一次显示
         */
        private static boolean isFirstShown = true;

        public AlertDialogHolder(final Context context) {
            dialog = new AlertDialog.Builder(context).setTitle("请求WRITE_SETTING权限")
                    .setNegativeButton("取消", null).setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT > 22) {
                                        // 想要请求WRITE_SETTINGS权限，必须通过Intent的方式
                                        Intent settingIntent = new Intent();
                                        settingIntent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        if (!(context instanceof Activity)) {
                                            // 从非Activity的Context中启动一个Activity，需要加上这个标志
                                            settingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        }
                                        context.startActivity(settingIntent);
                                    }
                                }
                            }
                    ).create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    isShowing = false;
                }
            });
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    isShowing = false;
                }
            });
            dialog.setCanceledOnTouchOutside(false);
        }

        public void show() {
            if (isShowing) {
                return;
            }
            isShowing = true;

            if (isFirstShown) {
                dialog.setMessage("快报的OmgIdManager需要WRITE_SETTING权限\n请在即将弹出的设置界面中开启该权限");
                isFirstShown = false;
            } else {
                dialog.setMessage("WRITE_SETTING权限开启失败\n为保证功能完善请您开启");
            }

            dialog.show();
        }

    }

    /**
     * 发起权限请求（本方法会检查是否需要向用户解释权限申请的原因）
     *
     * @param activity
     * @param requestCode
     * @param permissions
     */
    public static void requestPermissions(final Activity activity, final int requestCode, final String... permissions) {
        // 正在请求系统权限时，不能弹出 WRITE_SETTING 的请求对话框
        isRequestWriteSettingEnabled = false;

        if (shouldShowRequestPermissionRationale(activity, permissions)) { // 如果权限曾被拒绝（且尚未勾选“不再提醒”），则走这里
            // 此处要弹出一些UI交互界面，跟用户解释为何需要这个权限
            new AlertDialog.Builder(activity)
                    .setTitle("权限请求说明：")
                    .setMessage("腾讯新闻在使用某些特定功能时\n会向您申请电话、位置、存储、麦克风等权限")
                    .setPositiveButton("朕知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestNeededPermissions(activity, requestCode, permissions);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .create().show();
        } else { // 如果是第一次申请权限，或用户曾勾选了“不在提醒”，则走这里
            requestNeededPermissions(activity, requestCode, permissions);
        }
    }

    /* 私有辅助方法 */

    /**
     * 用于校验权限申请的结果<br/>
     *
     * @param grantResults 授权结果数组（Activity的onRequestPermissionsResult回调中会带过来）
     * @return true：申请的所有权限都获取到了；<br>false：至少有一个申请的权限被拒绝了
     */
    private static boolean verifyPermissionResults(int... grantResults) {
        if (grantResults == null || grantResults.length < 1) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查权限数组是否需要显示使用权限的说明<br/>
     *
     * @param activity    activity
     * @param permissions permission list 权限数组
     * @return true：至少有一项申请的权限需要进行权限申请的说明；<br>false：所有申请的权限都不需要进行说明
     */
    private static boolean shouldShowRequestPermissionRationale(Activity activity, String... permissions) {
        if (activity == null) {
            return false;
        }
        if (permissions == null || permissions.length < 1) {
            return false;
        }

        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发起权限请求（本方法会自动过滤掉已获得的权限，针对剩余的未获得的权限发起请求）<br>
     *
     * @param activity
     * @param requestCode
     * @param permissions
     */
    private static void requestNeededPermissions(Activity activity, int requestCode, String... permissions) {
        // 过滤出未获得的权限
        List<String> lackPermissionsList = new ArrayList<String>();
        for (String permission : permissions) {
            if (!PermissionUtil.checkSelfPermissions(activity, permission)) {
                lackPermissionsList.add(permission);
            }
        }
        // 仅当缺少权限时才发起请求；且如果屏幕上已有弹出的权限请求框时，不进行申请
        if (lackPermissionsList.size() > 0) {
            LogUtil.e("permission", "发起权限检查，数量：" + lackPermissionsList.size());
            ActivityCompat.requestPermissions(activity, lackPermissionsList.toArray(new String[lackPermissionsList.size()]), requestCode);
        }
    }

    /**
     * 获取权限的描述文字
     *
     * @param permission
     * @return
     */
    private static String getPermissionDescString(String permission) {
        String name = "";
        if (permission.equals(Manifest.permission.READ_PHONE_STATE)) {
            name = "电话";
        }
        if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            name = "存储";
        }
        if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            name = "位置";
        }
        if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
            name = "麦克风";
        }
        return name;
    }

}