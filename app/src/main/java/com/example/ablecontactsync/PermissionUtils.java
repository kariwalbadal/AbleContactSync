package com.example.ablecontactsync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    public static boolean isPermissionGranted(
            @NonNull Context context,
            @NonNull String permission
    ) {
        return ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
    }

    @NonNull
    public static List<String> checkAndGetRequiredPermissions(
            @NonNull Context context,
            String... permissions
    ) {

        List<String> pList = new ArrayList<>();

        for (String permission: permissions) {
            if(!isPermissionGranted(context, permission))
                pList.add(permission);
        }

        return pList;

    }

    @NonNull
    public static List<String> checkRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] granResults) {

        List<String> deniedPList = new ArrayList<>();

        for(int i = 0; i < permissions.length; ++i) {
            if(granResults[i] == PermissionChecker.PERMISSION_DENIED) {
                deniedPList.add(permissions[i]);
            }
        }

        return deniedPList;

    }

}
