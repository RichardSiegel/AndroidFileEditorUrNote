package de.tu_chemnitz.mi.android2.urnote;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private int PERMISSION_REQUEST_EXTERNAL_STORAGE;
    private String fileName = "";
    private String fileNameExtension = ".txt";
    private EditText editTitle, editNote;
    private ScrollView scrollViewNote;
    private ImageView imageView;
    private File fileToLoad;
    private String path;
    private TextView textView;

    private static final String TAG = String.valueOf(R.string.mainActivity);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTitle = (EditText) findViewById(R.id.editTextTitle);
        editNote = (EditText) findViewById(R.id.editTextNote);
        scrollViewNote = (ScrollView) findViewById(R.id.ScrollViewNote);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textViewNote);
        path = "";

        checkPermissionExternalStorage();

        Bundle intentValue = getIntent().getExtras();
        if (intentValue != null){
            switch ((FileBrowser.IoState) intentValue.get("ioState")) {
                case PATH_IS_FILE:
                    fileToLoad = (File) intentValue.get("file");
                    fileDecision(fileToLoad);
                    try {
                        Log.d(TAG, fileToLoad.getCanonicalPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case PATH_IS_FOLDER:
                    fileToLoad = (File) intentValue.get("file");
                    try {
                        path = fileToLoad.getCanonicalPath();
                        loadCacheAndSave(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                case PATH_IS_EMPTY:
                    Toast.makeText(this, R.string.noFileChosen, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    public void onClickFileBrowser(View view){
        if (editTitle.getText().length()==0)
            editTitle.setText(R.string.untitled);
        saveToCache();
        startActivity(new Intent(getApplicationContext(),FileBrowser.class));
    }

    public void onClickInternalSave(View view) {
        String path = getApplicationContext().getFilesDir().toString();
        if (editTitle.getText().length()==0)
            editTitle.setText(R.string.untitled);
        saveFile(editTitle.getText().toString(), editNote.getText().toString(), path, false);
        fileDecision(fileToLoad);
        Toast.makeText(getApplicationContext(), R.string.successfullySavedInternally, Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to write or overwrite a file.
     *
     * @param fileName file name without extension.
     * @param text file content.
     * @param path link to saving directory.
     * @param overwrite true, if overwrite is allowed.
     */
    private void saveFile(String fileName, String text, String path, Boolean overwrite){
        try {
            //neue Datei anlegen
            File fileToSave = new File(path + "/" + fileName + fileNameExtension);
            if (fileToSave.exists() && !overwrite){
                renameDialog(fileToSave.getName(),text,path);
            } else {
                //FileOutputStream anlegen und mit der angelegten Datei füttern
                FileOutputStream fos = new FileOutputStream(fileToSave);
                //Dateiinhalte beschreiben mit Text und umwandeln in ein Bytefeld
                fos.write(text.getBytes());
                //FileOutputStream schließen, damit keine Blockade beim späteren Zugriff auf die Datei entsteht
                fos.close();

                fileToLoad = fileToSave;
            }
        } catch (IOException e) {
            Toast.makeText(this, R.string.savingError, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Method to load a file
     */
    private void loadFromFile(){
        String note = "";
        try {
            //FileInputStream anlegen und mit der vorher gespeicherten Datei füttern
            FileInputStream fis = new FileInputStream(fileToLoad);
            //InputStreamReader liest Bytes und decodiert sie zu eine character Stream
            InputStreamReader isr = new InputStreamReader(fis);
            //BufferedReader kann character Stream in String umwandeln
            BufferedReader br = new BufferedReader(isr);
            //StringBuffer können zur Stringmanipulation verwendet werden
            StringBuffer sb = new StringBuffer();

            //lies String ein bis zum Ende der Zeile, hänge es im Stringbuffer an und füge einen Zeilenumbruch hinzu
            //so gewinnt man Zeilenumbrüche aus der Datei exakt wieder
            while ((note = br.readLine()) != null) {
                sb.append(note + "\n");
            }

            scrollViewNote.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            textView.setText(R.string.text);
            editTitle.setText(getFileName(fileToLoad));
            editNote.setText(sb);


            //FileInputStream schließen, damit keine Blockade beim späteren Zugriff auf die Datei entsteht
            fis.close();

        }catch (IOException e) {
            Toast.makeText(this, R.string.loadingError, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Method to load an image file
     */
    private void loadFromImageFile(){

        String note = "";
        try {
            FileInputStream fis = new FileInputStream(fileToLoad);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();

            while ((note = br.readLine()) != null) {
                sb.append(note + "\n");
            }

            scrollViewNote.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            editTitle.setText(getFileName(fileToLoad));
            textView.setText(R.string.image);

            Bitmap bMap = BitmapFactory.decodeFile(fileToLoad.getPath());
            imageView.setImageBitmap(bMap);

            fis.close();
        }catch (IOException e) {
            Toast.makeText(this,R.string.loadingError, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }



    //////////////////////////////////
    // zusätzliche Methoden der App //
    //////////////////////////////////
    private void renameDialog(String filename, final String text, final String path) {
        final EditText editTextDialog = new EditText(this);
        editTextDialog.setText(
        filename.copyValueOf(filename.toCharArray(),0,filename.lastIndexOf(".")).toString()
        );

        new AlertDialog.Builder(this)
                .setTitle(R.string.file_exists)
                .setMessage(R.string.enter_another_name)
                .setView(editTextDialog)
                .setPositiveButton(R.string.override_file,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            saveFile(editTextDialog.getText().toString(), text, path, true);
                            }
                        })
                .setNegativeButton(R.string.rename_file_or_folder,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            saveFile(editTextDialog.getText().toString(), text, path, false);
                            }
                        })
                .show();
    }

    private void fileDecision(File file) {
        Log.d(TAG, getFileExtension(file).toUpperCase());
        switch (getFileExtension(file)){
            case "png":
                loadFromImageFile();
                break;
            case "jpg":
                loadFromImageFile();
                break;
            case "mp3":
                //loadFromAudioFile();
        default:
            loadFromFile();
            break;
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    private String getFileName(File file){
        String name = file.getName();
        try {
            return name.substring(0, name.lastIndexOf("."));
        } catch (Exception e) {
            return "";
        }
    }

    private void saveToCache(){
        String path = getApplicationContext().getCacheDir().toString();
        saveFile("cache", editTitle.getText().toString() + "\r\n " + editNote.getText().toString(), path, true);
        Log.d(TAG, R.string.savedToCache + path);
    }

    private void loadCacheAndSave(String path){
        String note = "";
        String cachedFileName = "";
        File file = new File(getApplicationContext().getCacheDir().toString() + "/" + "cache.txt");

        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();

            cachedFileName = br.readLine();

            while ((note = br.readLine()) != null) {
                sb.append(note + "\n");
            }

            saveFile(cachedFileName, sb.toString(), path, false);
            Toast.makeText(getApplicationContext(), R.string.successfullySavedExternally, Toast.LENGTH_SHORT).show();
            Log.d(TAG, R.string.loadedFromCache + file.getCanonicalPath());
            fileDecision(fileToLoad);

            fis.close();
        }catch (IOException e) {
            Toast.makeText(this,  R.string.loadingCacheError, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    //Methode um eine Popup Nachricht zu generieren für die Nachfrage nach External Storage Permission (ab Android 6 notwendig)
    private void checkPermissionExternalStorage(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_EXTERNAL_STORAGE);
            }
        }
    }
}







