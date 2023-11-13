package io.github.doubi88.slideshowwallpaper.preferences;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ImageLoader;

public class SingleImageSelectorActivity extends AppCompatActivity {
    private SharedPreferencesManager manager;
    ImageView imageView = null;
    private static final int REQUEST_CODE_FILE = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_image_selector);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        manager = new SharedPreferencesManager(getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE));

        imageView = findViewById(R.id.imageview_main_wallpaper);
        String mainWallpaperPath = manager.getMainWallpaperPath();
        if (mainWallpaperPath != null) {
            Uri uri = Uri.parse(manager.getMainWallpaperPath());
            boolean takePermissionSuccess = takePermission(uri);
            if (takePermissionSuccess) {
                ImageInfo imageInfo = null;
                try {
                    imageInfo = ImageLoader.loadImage(uri, imageView.getContext(), imageView.getMaxWidth(), imageView.getMaxHeight(), false);
                } catch (IOException e) {
                    Log.e(SingleImageSelectorActivity.class.getSimpleName(), e.toString());
                }
                if (imageInfo != null) {
                    Bitmap image = imageInfo.getImage();
                    if (image != null) {
                        imageView.setImageBitmap(Bitmap.createBitmap(image, 0, 0,
                                image.getWidth(), image.getHeight()));
                    }
                }
            }
        }

        findViewById(R.id.btn_sel_main_wallpaper).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                intent.setAction(Intent.ACTION_GET_CONTENT);
            }
            startActivityForResult(intent, REQUEST_CODE_FILE);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                Uri uri = data.getData();
                if (uri != null) {
                    boolean takePermissionSuccess = takePermission(uri);
                    if (takePermissionSuccess) {
                        manager.setMainWallpaperPath(uri.toString());
                        ImageInfo i = null;
                        try {
                            i = ImageLoader.loadImage(uri, imageView.getContext(), imageView.getWidth(), imageView.getHeight(),false);
                        } catch (Exception e) {
                            Log.e(SingleImageSelectorActivity.class.getSimpleName(), e.toString());
                        }
                        if (i != null) {
                            Bitmap image = i.getImage();
                            if (image != null) {
                                imageView.setImageBitmap(Bitmap.createBitmap(image, 0, 0,
                                        image.getWidth(), image.getHeight()));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean takePermission(Uri uri) {
        boolean takePermissionSuccess = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver res = getContentResolver();
            int perms = res.getPersistedUriPermissions().size();
            res.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            int perms2 = res.getPersistedUriPermissions().size();
            // If size not increased, maybe get permission failed, or already granted.
            // We need check it.
            if (perms2 <= perms) {
                List<UriPermission> permissionList = res.getPersistedUriPermissions();
                for (UriPermission permission: permissionList) {
                    if (uri.equals(permission.getUri())) {
                        return true;
                    }
                }
                takePermissionSuccess = false;
            }
        }
        return takePermissionSuccess;
    }
}
