package de.tu_chemnitz.mi.android2.urnote;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class returns folder or file, if selected. It uses Intent-Extras to do so.
 * Methods like move, delete, and create new folder are demonstrated too.
 * Please feel free to alter, enhance and redistribute this class.
 *
 * @author Richard Siegel
 */
public class FileBrowser extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public enum IoState {
        PATH_IS_FOLDER,
        PATH_IS_FILE,
        PATH_IS_EMPTY
    }
    private enum SelectAction {
        OPEN,
        RENAME,
        DELETE,
        MOVE
    }
    private SelectAction selectAction;  // What happens when an item is selected.
    private Intent intentSendFile;
    private DrawerLayout drawerPlaces;  // Select USB, SD or Internal Storage here.
    private ListView listViewFiles;
    private TextView textViewURI;
    private Button buttonSave;
    private File filePath;
    private File fileCache;
    private ArrayList<String> fileNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerPlaces = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerPlaces, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerPlaces.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fileCache = null;
        selectAction = SelectAction.OPEN;
        intentSendFile = new Intent(this, MainActivity.class);
        buttonSave = (Button)findViewById(R.id.saveBtn);
        listViewFiles = (ListView)findViewById(R.id.list_of_files);
        textViewURI = (TextView)findViewById(R.id.textViewURI);
        adapter = new ArrayAdapter<String>(listViewFiles.getContext(),android.R.layout.simple_list_item_1, fileNames);
        drawerPlaces.openDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (!selectAction.equals(SelectAction.OPEN) && !selectAction.equals(SelectAction.MOVE)){
            waitForSelection(false);
        } else {
            if (filePath == null){
                goBack();
            } else {
                if (filePath.getParentFile().canRead() && filePath.getParentFile().exists()){
                    onClickListFileSystem("..");
                } else {
                    goBack();
                }
            }
        }
    }
    private void goBack() {
        if (!drawerPlaces.isDrawerOpen(GravityCompat.START)) {
            drawerPlaces.openDrawer(GravityCompat.START);
        } else {
            sendIntent(IoState.PATH_IS_EMPTY);
        }
    }

    public void onClickBackground(View v){
        if (!drawerPlaces.isDrawerOpen(GravityCompat.START) && filePath == null) {
            drawerPlaces.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_browser, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (filePath == null){
            Toast.makeText(this, R.string.you_must_select_a_drive, Toast.LENGTH_SHORT).show();
            onClickBackground(this.getCurrentFocus());
        } else {
            switch (item.getItemId()){
                case R.id.action_new_folder: {
                    mkdir();
                } break;
                case R.id.action_rename: {
                    waitForSelection(true);
                    selectAction = SelectAction.RENAME;
                } break;
                case R.id.action_delete: {
                    waitForSelection(true);
                    selectAction = SelectAction.DELETE;
                } break;
                case R.id.action_move: {
                    waitForMoveDest(true);
                    selectAction = SelectAction.MOVE;
                } break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    private void waitForSelection(boolean selectOn){
        if (selectOn){
            textViewURI.setText(R.string.select_file_or_folder);
            buttonSave.setEnabled(false);
            buttonSave.setVisibility(View.INVISIBLE);
        } else {
            selectAction = SelectAction.OPEN;
            textViewURI.setText(filePath.getPath());
            buttonSave.setEnabled(true);
            buttonSave.setVisibility(View.VISIBLE);
        }
    }
    private void waitForMoveDest(boolean movingFiles) {
        if (movingFiles){
            textViewURI.setText(R.string.select_file_or_folder);
            buttonSave.setVisibility(View.INVISIBLE);
            buttonSave.setText(R.string.paste_file);
        } else {
            fileCache = null;
            selectAction = SelectAction.OPEN;
            buttonSave.setText(R.string.save_file);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_appInternalFiles: loadListFileSystem(getApplicationContext().getFilesDir()); break;
            case R.id.nav_sdCard:           loadListFileSystem(Environment.getExternalStorageDirectory()); break;
            case R.id.nav_usb:              loadListFileSystem(Environment.getExternalStorageDirectory().getParentFile()); break;
            case R.id.nav_images:           loadListFileSystem(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)); break;
            case R.id.nav_music:            loadListFileSystem(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)); break;
            case R.id.nav_docs:             loadListFileSystem(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)); break;
            case R.id.nav_downloads:        loadListFileSystem(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)); break;
        }
        if (filePath.exists() && filePath.canRead() && selectAction.equals(SelectAction.OPEN)) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            buttonSave.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private void loadListFileSystem(File file){
        filePath = file;
        if (filePath.exists()) {
            listViewFiles.setAdapter(adapter);
            onClickListFileSystem("");
            listViewFiles.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    onClickListFileSystem(filePath.list()[position]);
                }
            });
        } else {
            Toast.makeText(FileBrowser.this, R.string.not_found, Toast.LENGTH_SHORT).show();
        }
    }
    private void onClickListFileSystem(String dirName){
        String uri = "/";
        try {
            if (dirName.equals("..")){
                uri = filePath.getParent();
            } else {
                uri = filePath.getCanonicalPath()+"/"+dirName;
            }
            filePath = new File(uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (filePath.canRead()) {
            switch (selectAction){
                case OPEN: openFile(uri); break;
                case MOVE: moveCache(uri); break;
                case RENAME: rename(); break;
                case DELETE: delete(); break;
            }
        } else {
            Toast.makeText(FileBrowser.this, R.string.not_readable, Toast.LENGTH_SHORT).show();
            filePath = filePath.getParentFile();
        }
        adapter.notifyDataSetChanged();
    }
    private void openFile(String uri) {
        if (filePath.isDirectory()) {
            if (!selectAction.equals(SelectAction.MOVE)) textViewURI.setText(uri);
            fileNames.clear();
            for (int i = 0; i <= filePath.list().length - 1; i++) {
                if (filePath.listFiles()[i].isDirectory()) {
                    fileNames.add("[DIR] - " + filePath.list()[i]);
                } else {
                    fileNames.add("[file] - " + filePath.list()[i] + "   < " + String.valueOf(filePath.listFiles()[i].length()) + " >");
                }
            }
        } else {
            switch (selectAction){
                case OPEN: sendIntent(IoState.PATH_IS_FILE); break;
                case MOVE: moveFile(); break;
            }
        }
    }
    private void moveCache(String uri) {
        if (fileCache != null){
            openFile(uri);
        } else {
            fileCache = filePath;
            filePath = filePath.getParentFile();
            textViewURI.setText(R.string.select_drive_or_folder);
            Toast.makeText(this, R.string.select_drive_or_folder, Toast.LENGTH_SHORT).show();
            buttonSave.setVisibility(View.VISIBLE);
        }
    }
    private void rename(){
        if (filePath.canWrite()){
            final EditText editTextDialog = new EditText(this);
            editTextDialog.setHint(filePath.getName());

            new AlertDialog.Builder(this)
                    .setTitle(R.string.rename_file_or_folder)
                    .setMessage(R.string.enter_a_name)
                    .setView(editTextDialog)
                    .setPositiveButton(R.string.dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    waitForSelection(false);
                                    try {
                                        filePath.renameTo(new File(filePath.getParentFile().getCanonicalPath() + "/" + editTextDialog.getText().toString()));
                                        loadListFileSystem(filePath.getParentFile());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    waitForSelection(false);
                                    Toast.makeText(FileBrowser.this, R.string.canceled, Toast.LENGTH_SHORT).show();
                                    loadListFileSystem(filePath.getParentFile());
                                }
                            })
                    .show();
        } else {
            Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_LONG).show();
        }
    }
    private void delete(){
        if (filePath.canWrite()){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete)
                    .setMessage(filePath.getName())
                    .setPositiveButton(R.string.dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    waitForSelection(false);
                                    deleteRecursive(filePath);
                                    loadListFileSystem(filePath.getParentFile());
                                }
                            })
                    .setNegativeButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    waitForSelection(false);
                                    Toast.makeText(FileBrowser.this, R.string.canceled, Toast.LENGTH_SHORT).show();
                                    loadListFileSystem(filePath.getParentFile());
                                }
                            })
                    .show();
        } else {
            Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_LONG).show();
        }
    }
    private void mkdir(){
        if (filePath.canWrite()){
            final EditText editTextDialog = new EditText(this);
            editTextDialog.setHint(R.string.untitled);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.new_folder)
                    .setMessage(R.string.enter_a_name)
                    .setView(editTextDialog)
                    .setPositiveButton(R.string.dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    try {
                                        new File(filePath.getCanonicalPath() + "/" + editTextDialog.getText().toString()).mkdir();
                                        loadListFileSystem(filePath);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast.makeText(FileBrowser.this, R.string.canceled, Toast.LENGTH_SHORT).show();
                                }
                            })
                    .show();
        } else {
            Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_LONG).show();
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    public void onClickSave(View v){
        switch (selectAction){
            case MOVE: moveFile(); break;
            case OPEN: sendIntent(IoState.PATH_IS_FOLDER); break;
        }
    }
    private void moveFile() {
        String fileName;
        if (fileCache.exists()){
            fileName = fileCache.getName();
            if (fileCache.canWrite() && filePath.canWrite()){
                File newPath = new File(filePath.getPath() + "/" + fileName);
                fileCache.renameTo(newPath);
                if (newPath.exists()){
                    Toast.makeText(this, R.string.file_moved, Toast.LENGTH_SHORT).show();
                    fileNames.add(fileName);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_LONG).show();
            }
            waitForMoveDest(false);
        } else {
            Toast.makeText(this, R.string.select_file_or_folder, Toast.LENGTH_SHORT).show();
        }
    }
    private void sendIntent(IoState state) {
        intentSendFile.putExtra("file", filePath);
        intentSendFile.putExtra("ioState",state);
        startActivity(intentSendFile);
    }

}


