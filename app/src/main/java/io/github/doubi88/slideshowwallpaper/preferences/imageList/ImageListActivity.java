/*
 * Slideshow Wallpaper: An Android live wallpaper displaying custom images.
 * Copyright (C) 2022  Doubi88 <tobis_mail@yahoo.de>
 *
 * Slideshow Wallpaper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Slideshow Wallpaper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package io.github.doubi88.slideshowwallpaper.preferences.imageList;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;

import java.util.LinkedList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;

public class ImageListActivity extends AppCompatActivity {

    private SharedPreferencesManager manager;
    private static final int REQUEST_CODE_FILE = 1;

    private ImageListAdapter imageListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        RecyclerView recyclerView = findViewById(R.id.image_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        manager = new SharedPreferencesManager(getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE));

        List<Uri> uris = manager.getImageUris(SharedPreferencesManager.Ordering.SELECTION);

        imageListAdapter = new ImageListAdapter(uris);
        imageListAdapter.addOnDeleteClickListener(info -> {
            manager.removeUri(info.getUri());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && info.getSize() > 0 && !manager.hasImageUri(info.getUri())) {
                getContentResolver().releasePersistableUriPermission(info.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        });
        recyclerView.setAdapter(imageListAdapter);

        findViewById(R.id.add_button).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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

        List<Uri> uris = new LinkedList<>();
        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                Uri uri = data.getData();
                if (uri != null) {
                    boolean takePermissionSuccess = takePermission(uri);
                    if (takePermissionSuccess && manager.addUri(uri)) {
                        uris.add(uri);
                    }
                }
            } else {
                for (int index = 0; index < clipData.getItemCount(); index++) {
                    Uri uri = clipData.getItemAt(index).getUri();
                    boolean takePermissionSuccess = takePermission(uri);
                    if (takePermissionSuccess && manager.addUri(uri)) {
                        uris.add(uri);
                    }
                }
            }

            imageListAdapter.addUris(uris);
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
