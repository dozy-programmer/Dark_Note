package com.akapps.dailynote.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.akapps.dailynote.R;
import com.akapps.dailynote.adapter.IconMenuAdapter;
import com.akapps.dailynote.classes.data.Backup;
import com.akapps.dailynote.classes.data.CheckListItem;
import com.akapps.dailynote.classes.data.Note;
import com.akapps.dailynote.classes.data.Photo;
import com.akapps.dailynote.classes.data.User;
import com.akapps.dailynote.classes.helpers.AppData;
import com.akapps.dailynote.classes.helpers.Helper;
import com.akapps.dailynote.classes.helpers.RealmBackupRestore;
import com.akapps.dailynote.classes.helpers.RealmDatabase;
import com.akapps.dailynote.classes.other.AccountSheet;
import com.akapps.dailynote.classes.other.CreditsSheet;
import com.akapps.dailynote.classes.other.IconPowerMenuItem;
import com.akapps.dailynote.classes.other.InfoSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.skydoves.powermenu.CustomPowerMenu;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;
import kotlin.io.FilesKt;
import www.sanju.motiontoast.MotionToast;

public class SettingsScreen extends AppCompatActivity{

    // activity
    private Context context;
    private int all_Notes;
    private boolean tryAgain;
    private int upgradeToProCounter;

    private User currentUser;
    public Realm realm;

    // account authentication
    private FirebaseAuth mAuth;

    // Toolbar
    private Toolbar toolbar;
    private ImageView close;

    // layout
    private LinearLayout backup;
    private LinearLayout restoreBackup;
    private LinearLayout backupBeta;
    private LinearLayout restoreBackupBeta;
    private LinearLayout appSettings;
    private LinearLayout syncLayout;
    private MaterialCardView contact;
    private MaterialCardView review;
    private TextView titleLines;
    private TextView previewLines;
    private LinearLayout titleLayout;
    private LinearLayout previewLayout;
    private MaterialCardView accountLayout;
    private TextView accountText;
    private CustomPowerMenu linesMenu;
    private boolean isTitleSelected;
    private SwitchCompat showPreview;
    private SwitchCompat showPreviewNoteInfo;
    private SwitchCompat openFoldersOnStart;
    private SwitchCompat showFolderNotes;
    private SwitchCompat modeSetting;
    private SwitchCompat sublistMode;
    private MaterialCardView grid;
    private MaterialCardView row;
    private MaterialCardView staggered;
    private TextView about;
    private MaterialButton signUp;
    private MaterialButton logIn;
    private MaterialButton sync;
    private MaterialButton upload;
    private TextView accountInfo;
    private TextView lastUploadDate;
    private Dialog progressDialog;
    private ImageView spaceOne;
    private ImageView spaceTwo;

    // variables
    private boolean betaBackup  = false;
    private boolean betaRestore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_screen);

        context = this;
        mAuth = FirebaseAuth.getInstance();
        all_Notes = getIntent().getIntExtra("size", 0);
        boolean backingUp = getIntent().getBooleanExtra("backup", false);

        try {
            realm = Realm.getDefaultInstance();
        } catch (Exception e) {
            realm = RealmDatabase.setUpDatabase(context);
        }
        currentUser = realm.where(User.class).findFirst();

        populateUserSettings();

        if(backingUp)
            showBackupRestoreInfo(6);
    }

    @Override
    protected void onResume() {
        super.onResume();
        realmStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(realm!=null)
            realm.close();
    }

    @Override
    public void onBackPressed() {
       close();
    }

    private void realmStatus(){
        if(realm.isClosed()) {
            try {
                realm = Realm.getDefaultInstance();
            } catch (Exception e) {
                realm = RealmDatabase.setUpDatabase(context);
            }
            currentUser = realm.where(User.class).findFirst();
        }
    }

    private void initializeLayout(){
        toolbar = findViewById(R.id.toolbar);
        close = findViewById(R.id.close_activity);
        backup = findViewById(R.id.backup);
        restoreBackup = findViewById(R.id.restore_backup);
        appSettings = findViewById(R.id.app_settings);
        contact = findViewById(R.id.contact);
        review = findViewById(R.id.review);
        about = findViewById(R.id.about);
        titleLines = findViewById(R.id.title_lines);
        previewLines = findViewById(R.id.preview_lines);
        titleLayout = findViewById(R.id.title_layout);
        previewLayout = findViewById(R.id.preview_layout);
        showPreview = findViewById(R.id.show_preview_switch);
        showPreviewNoteInfo = findViewById(R.id.show_info_switch);
        openFoldersOnStart = findViewById(R.id.open_folder_switch);
        showFolderNotes = findViewById(R.id.show_folder_switch);
        modeSetting = findViewById(R.id.mode_setting);
        sublistMode = findViewById(R.id.sublists_switch);
        grid = findViewById(R.id.grid);
        row = findViewById(R.id.row);
        staggered = findViewById(R.id.staggered);
        backupBeta = findViewById(R.id.backup_beta);
        restoreBackupBeta = findViewById(R.id.restore_beta_backup);
        syncLayout = findViewById(R.id.logged_in_layout);
        signUp = findViewById(R.id.sign_up);
        logIn = findViewById(R.id.log_in);
        sync = findViewById(R.id.sync);
        upload = findViewById(R.id.upload);
        accountInfo = findViewById(R.id.account_name);
        lastUploadDate = findViewById(R.id.last_upload);
        spaceOne = findViewById(R.id.space_one);
        spaceTwo = findViewById(R.id.space_two);
        accountLayout = findViewById(R.id.account_layout);
        accountText = findViewById(R.id.account_settings);

        if(Helper.isPortrait(context)){
            MaterialCardView coffee = findViewById(R.id.coffee_button);
            TextView coffeeText = findViewById(R.id.support_me_message);
            coffeeText.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/akapps"));
                startActivity(browserIntent);
            });
            coffee.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/akapps"));
                startActivity(browserIntent);
            });
        }

        Helper.moveBee(findViewById(R.id.version_icon), 200f);
        logIn.setBackgroundColor(context.getColor(R.color.darker_blue));

        if(null == currentUser.getEmail()){
            realm.beginTransaction();
            currentUser.setEmail("");
            realm.commitTransaction();
        }

        if(currentUser.isUltimateUser()) {
            accountLayout.setVisibility(View.VISIBLE);
            accountText.setVisibility(View.VISIBLE);
            if(mAuth.getCurrentUser() != null){
                if(mAuth.getCurrentUser().isEmailVerified()) {
                    signUp.setVisibility(View.GONE);
                    logIn.setText("Log Out");
                    syncLayout.setVisibility(View.VISIBLE);
                    logIn.setBackgroundColor(context.getColor(R.color.red));
                    sync.setBackgroundColor(context.getColor(R.color.darker_blue));
                    upload.setBackgroundColor(context.getColor(R.color.gold));
                    upload.setTextColor(context.getColor(R.color.gray));
                    accountInfo.setVisibility(View.VISIBLE);
                    accountInfo.setText(mAuth.getCurrentUser().getEmail());
                    spaceOne.setVisibility(View.VISIBLE);
                    spaceTwo.setVisibility(View.VISIBLE);

                    if (null != currentUser.getLastUpload() && !currentUser.getLastUpload().isEmpty()) {
                        lastUploadDate.setVisibility(View.VISIBLE);
                        lastUploadDate.setText("Last Upload : " + currentUser.getLastUpload().replaceAll("\n", " "));
                    }
                }
            }
        }
        else{
            accountLayout.setVisibility(View.GONE);
            accountText.setVisibility(View.GONE);
        }

        String titleLinesNumber = String.valueOf(currentUser.getTitleLines());
        String previewLinesNumber = String.valueOf(currentUser.getContentLines());

        // sets the current select title lines and preview lines
        // by default it is 3
        titleLines.setText(titleLinesNumber);
        previewLines.setText(previewLinesNumber);

        // toolbar
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        signUp.setOnClickListener(view -> {
            realmStatus();
            if(currentUser.isUltimateUser()) {
                if (mAuth.getCurrentUser() == null) {
                    AccountSheet accountLoginSheet = new AccountSheet(mAuth, currentUser, realm, true);
                    accountLoginSheet.show(getSupportFragmentManager(), accountLoginSheet.getTag());
                }
            }
        });

        logIn.setOnClickListener(view -> {
            realmStatus();
            if (mAuth.getCurrentUser() != null) {
                showBackupRestoreInfo(8);
            } else {
                AccountSheet accountLoginSheet = new AccountSheet(mAuth, currentUser, realm, false);
                accountLoginSheet.show(getSupportFragmentManager(), accountLoginSheet.getTag());
            }
        });

        sync.setOnClickListener(view -> {
            realmStatus();
            if (mAuth.getCurrentUser() != null && currentUser.isUltimateUser())
                showBackupRestoreInfo(7);
        });

        upload.setOnClickListener(view -> {
            realmStatus();
            if (mAuth.getCurrentUser() != null && currentUser.isUltimateUser())
                showBackupRestoreInfo(6);
        });

        backup.setOnClickListener(v -> {
            realmStatus();
            betaBackup = false;
            showBackupRestoreInfo(1);
        });

        backupBeta.setOnClickListener(view -> {
            realmStatus();
            betaBackup = true;
            showBackupRestoreInfo(2);
        });

        restoreBackup.setOnClickListener(v -> {
            realmStatus();
            betaRestore = false;
            openFile();
        });

        restoreBackupBeta.setOnClickListener(view -> {
            betaRestore = true;
            openFile();
        });

        titleLayout.setOnClickListener(v -> {
            realmStatus();
            isTitleSelected = true;
            showLineNumberMenu(titleLines, null);
        });

        previewLayout.setOnClickListener(v -> {
            realmStatus();
            isTitleSelected = false;
            showLineNumberMenu(previewLines, null);
        });

        appSettings.setOnClickListener(v -> openAppInSettings());

        row.setOnClickListener(v -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setLayoutSelected("row");
            realm.commitTransaction();
            row.setCardBackgroundColor(context.getColor(R.color.darker_blue));
            grid.setCardBackgroundColor(context.getColor(R.color.gray));
            staggered.setCardBackgroundColor(context.getColor(R.color.gray));
        });

        grid.setOnClickListener(v -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setLayoutSelected("grid");
            realm.commitTransaction();
            grid.setCardBackgroundColor(context.getColor(R.color.darker_blue));
            row.setCardBackgroundColor(context.getColor(R.color.gray));
            staggered.setCardBackgroundColor(context.getColor(R.color.gray));
        });

        staggered.setOnClickListener(v -> {
            realmStatus();
            if(!currentUser.getLayoutSelected().equals("stag")) {
                realm.beginTransaction();
                currentUser.setLayoutSelected("stag");
                realm.commitTransaction();
                staggered.setCardBackgroundColor(context.getColor(R.color.darker_blue));
                grid.setCardBackgroundColor(context.getColor(R.color.gray));
                row.setCardBackgroundColor(context.getColor(R.color.gray));
            }
        });

        contact.setOnClickListener(v -> contactMe());

        review.setOnClickListener(v -> openAppInPlayStore());

        close.setOnClickListener(v -> close());

        showPreview.setOnCheckedChangeListener((buttonView, isChecked) -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setShowPreview(isChecked);
            realm.commitTransaction();
        });

        showPreviewNoteInfo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setShowPreviewNoteInfo(isChecked);
            realm.commitTransaction();
        });

        openFoldersOnStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppData.isAppFirstStarted = false;
            realmStatus();
            realm.beginTransaction();
            currentUser.setOpenFoldersOnStart(isChecked);
            realm.commitTransaction();
        });

        showFolderNotes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setShowFolderNotes(isChecked);
            realm.commitTransaction();
        });

        modeSetting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setModeSettings(isChecked);
            realm.commitTransaction();
            checkModeSettings();
        });

        sublistMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            realmStatus();
            realm.beginTransaction();
            currentUser.setEnableSublists(isChecked);
            realm.where(Note.class).findAll().setBoolean("enableSublist", true);
            realm.commitTransaction();
        });

        about.setOnClickListener(v -> {
            upgradeToProCounter++;
            if(upgradeToProCounter == 1) {
                CreditsSheet creditSheet = new CreditsSheet();
                creditSheet.show(getSupportFragmentManager(), creditSheet.getTag());
            }
        });

        about.setOnLongClickListener(v -> {
            if(upgradeToProCounter == 12)
                upgradeToPro();
            return false;
        });
    }

    private void upgradeToPro(){
        realm.beginTransaction();
        currentUser.setProUser(!currentUser.isProUser());
        currentUser.setUltimateUser(true);
        currentUser.setEnableSublists(true);
        realm.commitTransaction();
        if(currentUser.isProUser()) {
            Helper.showMessage(SettingsScreen.this, "Upgrade Successful", "" +
                    "Thank you and Enjoy!\uD83D\uDE04", MotionToast.TOAST_SUCCESS);
        }
        else{
            Helper.showMessage(SettingsScreen.this, "Downgrade Successful", "" +
                    "Enjoy!\uD83D\uDE04", MotionToast.TOAST_SUCCESS);
        }
      restart();
    }

    private void populateUserSettings(){
        initializeLayout();
        initializeSettings();
    }

    private void checkModeSettings(){
        if(currentUser.isModeSettings()) {
            modeSetting.setText("Light Mode  ");
            modeSetting.setTextColor(context.getColor(R.color.ultra_white));
            AppData.getAppData().isLightMode = true;
            updateGapLayoutColor();
            getWindow().setStatusBarColor(context.getColor(R.color.light_mode));
            ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(context.getColor(R.color.light_mode));
        }
        else {
            modeSetting.setText("Dark Mode  ");
            modeSetting.setTextColor(context.getColor(R.color.light_light_gray));
            AppData.getAppData().isLightMode = false;
            getWindow().setStatusBarColor(context.getColor(R.color.gray));
            updateGapLayoutColor();
            ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(context.getColor(R.color.gray));
        }
    }

    private void updateGapLayoutColor(){
        int gapColor = 0;
        if(AppData.getAppData().isLightMode)
            gapColor = context.getColor(R.color.light_mode);
        else
            gapColor = context.getColor(R.color.gray);

        findViewById(R.id.space_one).setBackgroundColor(gapColor);
        findViewById(R.id.space_two).setBackgroundColor(gapColor);
        findViewById(R.id.gap_one).setBackgroundColor(gapColor);
        findViewById(R.id.gap_two).setBackgroundColor(gapColor);
        findViewById(R.id.gap_three).setBackgroundColor(gapColor);
        findViewById(R.id.gap_four).setBackgroundColor(gapColor);
        findViewById(R.id.gap_five).setBackgroundColor(gapColor);
        findViewById(R.id.gap_six).setBackgroundColor(gapColor);
        findViewById(R.id.gap_seven).setBackgroundColor(gapColor);
    }

    private void initializeSettings(){
        showPreview.setChecked(currentUser.isShowPreview());
        showPreviewNoteInfo.setChecked(currentUser.isShowPreviewNoteInfo());
        openFoldersOnStart.setChecked(currentUser.isOpenFoldersOnStart());
        showFolderNotes.setChecked(currentUser.isShowFolderNotes());
        modeSetting.setChecked(currentUser.isModeSettings());
        sublistMode.setChecked(currentUser.isEnableSublists());
        checkModeSettings();

        if(currentUser.getLayoutSelected().equals("row"))
            row.setCardBackgroundColor(context.getColor(R.color.darker_blue));
        else if(currentUser.getLayoutSelected().equals("grid"))
            grid.setCardBackgroundColor(context.getColor(R.color.darker_blue));
        else
            staggered.setCardBackgroundColor(context.getColor(R.color.darker_blue));
    }

    public void openBackUpRestoreDialog(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
        else
            openBackup();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openBackup();
            else
                Helper.showMessage(this, "Accept Permission", "You need " +
                        "to accept permissions to backup", MotionToast.TOAST_ERROR);
        }
    }

    public void restart(){
        Intent intent = new Intent(SettingsScreen.this, SettingsScreen.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openBackup(){
        if(betaBackup)
            backUpDataAndImages();
        else
            backUpData();
    }

   private void backUpData(){
        if(all_Notes!=0) {
            realm.close();
            RealmBackupRestore realmBackupRestore = new RealmBackupRestore(this);
            realmBackupRestore.update(this, context);
            File exportedFilePath = realmBackupRestore.backup_Share();
            shareFile(exportedFilePath);
        }
        else
            Helper.showMessage(this, "Backup Failed", "\uD83D\uDE10No " +
                    "data to backup\uD83D\uDE10", MotionToast.TOAST_ERROR);
    }

    private void backUpDataAndImages(){
        if(all_Notes!=0)
            shareFile(new File(backUpZip()));
        else
            Helper.showMessage(this, "Backup Failed", "\uD83D\uDE10No " +
                    "data to backup\uD83D\uDE10", MotionToast.TOAST_ERROR);
    }

    private String backUpZip(){
        RealmResults<Note> checklistPhotos = realm.where(Note.class).findAll();
        ArrayList<String> allPhotos = getAllFilePaths(realm.where(Photo.class).findAll(), checklistPhotos);
        realm.close();
        RealmBackupRestore realmBackupRestore = new RealmBackupRestore(this);
        realmBackupRestore.update(this, context);
        File exportedFilePath = realmBackupRestore.backup_Share();
        allPhotos.add(exportedFilePath.getAbsolutePath());
        realmStatus();
        return zipPhotos(allPhotos);
    }

    private ArrayList<String> getAllFilePaths(RealmResults<Photo> allNotePhotos, RealmResults<Note> allNotes){
        ArrayList<String> allPhotos = new ArrayList<>();
        for(int i = 0; i < allNotePhotos.size(); i++)
            allPhotos.add(allNotePhotos.get(i).getPhotoLocation());

        for(int i=0; i< allNotes.size(); i++){
            RealmList<CheckListItem> currentNoteChecklist= allNotes.get(i).getChecklist();
            if(currentNoteChecklist.size() > 0){
                for(int j=0 ;j < currentNoteChecklist.size(); j++){
                    CheckListItem currentChecklistItem = currentNoteChecklist.get(j);
                    if(currentChecklistItem.getItemImage() != null && !currentChecklistItem.getItemImage().isEmpty())
                        allPhotos.add(currentChecklistItem.getItemImage());
                }
            }
        }

        return allPhotos;
    }

    // creates a zip folder
    public String createZipFolder(){
        File storageDir = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir.getAbsolutePath();
    }

    // places all the photos in a zip file and returns a string of the file path
    public String zipPhotos(ArrayList<String> files) {
        String zipPath = createZipFolder() + ".zip";
        File zipFile = new File(zipPath);

        int BUFFER = 1024;
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte[] data = new byte[BUFFER];
            for (int i = 0; i < files.size(); i++) {
                File newFile = new File(files.get(i));
                if(newFile.exists()) {
                    String canonicalPath = newFile.getCanonicalPath();
                    if (!canonicalPath.startsWith(zipPath.replace(".zip", ""))) {
                        throw new SecurityException("Zipping Error");
                    }
                    else {
                        FileInputStream fi = new FileInputStream(files.get(i));
                        origin = new BufferedInputStream(fi, BUFFER);
                        ZipEntry entry = new ZipEntry(files.get(i).substring(files.get(i).lastIndexOf("/") + 1));
                        out.putNextEntry(entry);
                        int count;
                        while ((count = origin.read(data, 0, BUFFER)) != -1) {
                            out.write(data, 0, count);
                        }
                        origin.close();
                    }
                }
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return zipFile.getAbsolutePath();
    }

    public void upLoadData(){
        File backupFile = new File(backUpZip());
        Uri file = Uri.fromFile(backupFile);
        // file info
        double fileSizeInMB = backupFile.length() / (Math.pow(1024, 2));
        fileSizeInMB = Double.parseDouble(new DecimalFormat("#.##").format(fileSizeInMB));

        progressDialog = Helper.showLoading("Uploading...\n" + fileSizeInMB + " MB",
                progressDialog, context, true);

        if(fileSizeInMB > 100){
            Helper.showMessage(this, "Upload Failed", "File size is too big, backup locally",
                    MotionToast.TOAST_ERROR);
            return;
        }

        String fileSizeString;
        if(fileSizeInMB < 1) {
            fileSizeInMB = backupFile.length() / 1024;
            fileSizeInMB = Double.valueOf(new DecimalFormat("#.##").format(fileSizeInMB));
            fileSizeString = fileSizeInMB + "_KB";
        }
        else
            fileSizeString = fileSizeInMB + "_MB";

        String currentDate = Helper.getBackupDate(fileSizeString);
        String fileName = currentDate + "_backup.zip";

        // check if file exists
        if(realm.where(Backup.class).equalTo("fileName", fileName).count() == 1){
            Helper.showLoading("", progressDialog, context, false);
            Helper.showMessage(this, "Upload Failed", "File name exists, " +
                    "please wait a minute and try again", MotionToast.TOAST_ERROR);
        }
        else {
            String userEmail = mAuth.getCurrentUser().getEmail();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference().child("users")
                    .child(userEmail)
                    .child(fileName);
            UploadTask uploadTask = storageRef.putFile(file);

            double finalFileSizeInMB = fileSizeInMB;
            uploadTask.addOnProgressListener(snapshot -> {
                double fileSizeInMBTransferred = Double.parseDouble(new DecimalFormat("##.##").format(snapshot.getBytesTransferred() / (Math.pow(1024, 2))));
                progressDialog = Helper.showLoading("Uploading...\n" + fileSizeInMBTransferred + " MB / " + finalFileSizeInMB + " MB",
                        progressDialog, context, true);
            }).addOnFailureListener(exception -> {
                progressDialog.cancel();
                Helper.showMessage(this, "Upload error",
                        "Error Uploading data, try again",
                        MotionToast.TOAST_ERROR);
                restart();
            }).addOnSuccessListener(taskSnapshot -> {
                double fileSizeInMBTransferred = Double.parseDouble(new DecimalFormat("##.##").format(taskSnapshot.getBytesTransferred() / (Math.pow(1024, 2))));
                if(fileSizeInMBTransferred == finalFileSizeInMB){
                    realm.beginTransaction();
                    realm.insert(new Backup(currentUser.getUserId(), fileName, new Date(), 0));
                    currentUser.setLastUpload(Helper.getCurrentDate());
                    lastUploadDate.setVisibility(View.VISIBLE);
                    lastUploadDate.setText("Last Upload : " + currentUser.getLastUpload().replaceAll("\n", " "));
                    realm.commitTransaction();
                    progressDialog.cancel();
                    Helper.showMessage(SettingsScreen.this, "Upload Success",
                            "Data Uploaded",
                            MotionToast.TOAST_SUCCESS);
                }
                else{
                    // deleting file since it was missing files
                    storageRef.delete().addOnSuccessListener(aVoid -> {})
                            .addOnFailureListener(exception -> {});
                    progressDialog.cancel();
                    Helper.showMessage(SettingsScreen.this, "Upload Error",
                            "Files missing, please upload again!",
                            MotionToast.TOAST_ERROR);
                }
            });
        }
    }

    private void showBackupRestoreInfo(int selection){
        InfoSheet info = new InfoSheet(selection);
        info.show(getSupportFragmentManager(), info.getTag());
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, 1);
    }

    private void shareFile(File backup){
        realmStatus();
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");

        Uri fileBackingUp  = FileProvider.getUriForFile(
                context,
                "com.akapps.dailynote.fileprovider",
                backup);
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileBackingUp);

        startActivity(emailIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(betaRestore)
                restoreBackupBeta(data);
            else
                restoreBackup(data);
        }
    }

    public void restoreFromDatabase(String fileName, double fileSize){
        progressDialog = Helper.showLoading("Syncing...", progressDialog, context, true);
        String userEmail = mAuth.getCurrentUser().getEmail();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference()
                .child("users/" + userEmail + "/" + fileName);

        File storageDir = new File(context
                .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/Dark Note");

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File localFile = new File(storageDir,"backup.zip");

        storageRef.getFile(localFile)
                .addOnProgressListener(snapshot -> {
                    double fileSizeInMBTransferred = Double.parseDouble(new DecimalFormat("###.#").format(snapshot.getBytesTransferred() / (Math.pow(1024, 2))));
                    progressDialog = Helper.showLoading("Syncing...\n" + fileSizeInMBTransferred + " MB / " + fileSize + " MB",
                            progressDialog, context, true);
                }).addOnSuccessListener(taskSnapshot -> {
            restoreFromDatabase(Uri.fromFile(localFile));
        }).addOnFailureListener(exception ->{
                Helper.showLoading("", progressDialog, context, false);
                Helper.showMessage(SettingsScreen.this, "Error", "" +
                "Restoring Error from database, please clear app storage & try again", MotionToast.TOAST_ERROR);
        });
    }

    private void restoreFromDatabase(Uri uri){
        RealmBackupRestore realmBackupRestore = new RealmBackupRestore(this);
        try {
            // close realm before restoring
            RealmConfiguration configuration = realm.getConfiguration();
            realm.close();
            Realm.deleteRealm(configuration);
            // initialize backup object
            realmBackupRestore = new RealmBackupRestore(this);
            // make a copy of the backup zip file selected by user and unzip it
            realmBackupRestore.restore(uri, "backup.zip", true);

            Helper.showLoading("", progressDialog, context, false);

            ArrayList<String> images = realmBackupRestore.getImagesPath();
            realmBackupRestore.copyBundledRealmFile(realmBackupRestore.getBackupPath(), "default.realm");

            Helper.showMessage(this, "Restored", "" + "Notes have been restored",
                    MotionToast.TOAST_SUCCESS);

            // update image paths from restored database so it knows where the images are
            realm = RealmDatabase.setUpDatabase(context);
            updateAlarms(realm.where(Note.class)
                    .equalTo("archived", false)
                    .equalTo("trash", false).findAll());
            updateImages(images);

            // delete all zip files
            Helper.deleteZipFile(context);
            close();
        } catch (Exception e) {
            Helper.showLoading("", progressDialog, context, false);
            Helper.showMessage(this, "Error", "" +
                    "Restoring Error to device, try again", MotionToast.TOAST_ERROR);
        }
    }

    private void restoreBackupBeta(Intent data){
        if (data != null) {
            Uri uri = data.getData();
            Cursor returnCursor = context.getContentResolver()
                    .query(uri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            String fileName = returnCursor.getString(nameIndex);

            if(fileName.contains(".zip")) {
                RealmBackupRestore realmBackupRestore = new RealmBackupRestore(this);
                try {
                    // close realm before restoring
                    RealmConfiguration configuration = realm.getConfiguration();
                    realm.close();
                    Realm.deleteRealm(configuration);

                    // delete all files first
                    FilesKt.deleteRecursively(new File(context.getExternalFilesDir(null) + ""));

                    // initialize backup object
                    realmBackupRestore = new RealmBackupRestore(this);
                    // make a copy of the backup zip file selected by user and unzip it
                    realmBackupRestore.restore(uri, "backup.zip", true);

                    ArrayList<String> images = realmBackupRestore.getImagesPath();
                    realmBackupRestore.copyBundledRealmFile(realmBackupRestore.getBackupPath(), "default.realm");

                    // update image paths from restored database so it knows where the images are
                    try {
                        realm = Realm.getDefaultInstance();
                    }
                    catch (Exception e){
                        realm = RealmDatabase.setUpDatabase(context);
                    }
                    updateAlarms(realm.where(Note.class)
                            .equalTo("archived", false)
                            .equalTo("trash", false).findAll());
                    updateImages(images);

                    Helper.showMessage(this, "Restored", "" +
                            "Notes have been restored", MotionToast.TOAST_SUCCESS);

                    Helper.showLoading("", progressDialog, context, false);

                    // delete all zip files
                    Helper.deleteZipFile(context);

                    close();
                } catch (Exception e) {
                    Helper.showLoading("", progressDialog, context, false);
                    if(tryAgain){
                        Helper.showMessage(this, "Error", "Clear storage via App Settings",
                                MotionToast.TOAST_ERROR);
                        appSettings.setBackgroundColor(getColor(R.color.flamingo));
                        tryAgain = false;
                    }
                    else {
                        Helper.showMessage(this, "Error","attempting to fix error...try again",
                                MotionToast.TOAST_ERROR);
                        tryAgain = true;
                    }
                }
            }
            else {
                Helper.showLoading("", progressDialog, context, false);
                Helper.showMessage(this, "Big Error\uD83D\uDE14", "" +
                        "Why are you trying to break my app (only .realm files)", MotionToast.TOAST_ERROR);
            }
        }
    }

    private void restoreBackup(Intent data){
        if (data != null) {
            Uri uri = data.getData();
            Cursor returnCursor = context.getContentResolver()
                    .query(uri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            String fileName = returnCursor.getString(nameIndex);

            if(fileName.contains(".realm")) {
                try {
                    RealmConfiguration configuration = realm.getConfiguration();
                    realm.close();
                    Realm.deleteRealm(configuration);

                    RealmBackupRestore realmBackupRestore = new RealmBackupRestore(this);
                    realmBackupRestore.restore(uri, "default.realm", false);
                    Helper.showMessage(this, "Restored", "" +
                            "Notes have been restored", MotionToast.TOAST_SUCCESS);

                    try {
                        realm = Realm.getDefaultInstance();
                    }
                    catch (Exception e){
                        realm = RealmDatabase.setUpDatabase(context);
                    }
                    updateAlarms(realm.where(Note.class)
                            .equalTo("archived", false)
                            .equalTo("trash", false).findAll());

                    Helper.showLoading("", progressDialog, context, false);

                    close();
                } catch (Exception e) {
                    if(tryAgain){
                        Helper.showMessage(this, "Error", "Clear storage via App Settings", MotionToast.TOAST_ERROR);
                        appSettings.setBackgroundColor(getColor(R.color.flamingo));
                        tryAgain = false;
                    }
                    else {
                        Helper.showMessage(this, "Error","attempting to fix error...try again", MotionToast.TOAST_ERROR);
                        tryAgain = true;
                    }
                }
            }
            else
                Helper.showMessage(this, "Big Error\uD83D\uDE14", "" +
                        "Why are you trying to break my app (only .realm files)", MotionToast.TOAST_ERROR);
        }
    }

    private void updateAlarms(RealmResults<Note> allNotes){
        for (int i=0; i < allNotes.size(); i++){
            Note currentNote = allNotes.get(i);
            if(null != currentNote.getReminderDateTime() && !currentNote.getReminderDateTime().isEmpty())
                Helper.startAlarm(this, currentNote, realm);
        }
    }

    private void updateImages(ArrayList<String> imagePaths){
        if(imagePaths.size() != 0) {
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i).substring(imagePaths.get(i).lastIndexOf("/") + 1);
                if(imagePaths.get(i).contains("~")){
                    CheckListItem currentChecklistPhoto = realm.where(CheckListItem.class).contains("itemImage", imagePath).findFirst();
                    if(currentChecklistPhoto != null) {
                        realm.beginTransaction();
                        currentChecklistPhoto.setItemImage(imagePaths.get(i));
                        realm.commitTransaction();
                    }
                }
                else{
                    Photo currentPhoto = realm.where(Photo.class).contains("photoLocation", imagePath).findFirst();
                    if(currentPhoto != null) {
                        realm.beginTransaction();
                        currentPhoto.setPhotoLocation(imagePaths.get(i));
                        realm.commitTransaction();
                    }
                }
            }
        }
    }

    private void showLineNumberMenu(TextView lines, SwitchCompat reminderDropDown){
        linesMenu = new CustomPowerMenu.Builder<>(context, new IconMenuAdapter(true))
            .addItem(new IconPowerMenuItem(null, "1"))
            .addItem(new IconPowerMenuItem(null, "2"))
            .addItem(new IconPowerMenuItem(null, "3"))
            .addItem(new IconPowerMenuItem(null, "4"))
            .addItem(new IconPowerMenuItem(null, "5"))
            .addItem(new IconPowerMenuItem(null, "6"))
            .addItem(new IconPowerMenuItem(null, "7"))
            .addItem(new IconPowerMenuItem(null, "8"))
            .setBackgroundColor(getColor(R.color.light_gray))
            .setOnMenuItemClickListener(onIconMenuItemClickListener)
            .setAnimation(MenuAnimation.SHOW_UP_CENTER)
            .setWidth(300)
            .setMenuRadius(15f)
            .setMenuShadow(10f)
            .build();

        linesMenu.showAsDropDown(lines);
    }

    private final OnMenuItemClickListener<IconPowerMenuItem> onIconMenuItemClickListener = new OnMenuItemClickListener<IconPowerMenuItem>() {
        @Override
        public void onItemClick(int position, IconPowerMenuItem item) {
            updateSelectedLines(position+1);
            linesMenu.dismiss();
        }
    };

    private void updateSelectedLines(int position){
        if(isTitleSelected) {
            realm.beginTransaction();
            currentUser.setTitleLines(position);
            realm.commitTransaction();
            titleLines.setText(String.valueOf(position));
        }
        else {
            realm.beginTransaction();
            currentUser.setContentLines(position);
            realm.commitTransaction();
            previewLines.setText(String.valueOf(position));
        }
    }

    private void openAppInPlayStore(){
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.akapps.dailynote"));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        }
        catch (Exception exception){}
    }

    private void contactMe(){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ak.apps.2019@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Dark Note App Feedback");
        startActivity(intent);
    }

    private void close(){
        Intent intent = new Intent(this, Homepage.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, R.anim.hide_to_bottom);
    }

    private void openAppInSettings(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        startActivity(intent);
    }
}