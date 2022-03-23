package com.akapps.dailynote.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.akapps.dailynote.R;
import com.akapps.dailynote.activity.CategoryScreen;
import com.akapps.dailynote.activity.NoteEdit;
import com.akapps.dailynote.activity.SettingsScreen;
import com.akapps.dailynote.classes.data.Folder;
import com.akapps.dailynote.classes.helpers.AppData;
import com.akapps.dailynote.classes.helpers.RealmDatabase;
import com.akapps.dailynote.classes.other.FilterSheet;
import com.akapps.dailynote.classes.data.User;
import com.akapps.dailynote.classes.helpers.Helper;
import com.akapps.dailynote.classes.data.Note;
import com.akapps.dailynote.recyclerview.notes_recyclerview;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.card.MaterialCardView;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import www.sanju.motiontoast.MotionToast;

public class notes extends Fragment{

    // layout
    private View view;
    private Context context;
    private TextView fragmentTitle;
    private TextView sortedBy;
    private CardView searchLayout;
    private CardView filterNotes;
    private CardView settings;
    private CardView restoreNotes;
    private CardView categoryNotes;
    private SearchView searchEditText;
    private ImageView search;
    private ImageView filterIcon;
    private RecyclerView recyclerViewNotes;
    private RecyclerView.Adapter adapterNotes;
    private FloatingActionButton addNote;
    private FloatingActionButton addCheckList;
    private FloatingActionMenu addMenu;
    // ad
    private MaterialCardView adLayout;
    private MaterialCardView upgradeButton;

    // on-device database
    private Realm realm;
    private RealmResults<Note> allNotes;
    private RealmResults<Note> filteredNotes;
    public User user;

    // activity data
    private boolean isSearchingNotes;
    private String searchingString;
    private boolean deletingMultipleNotes;
    private boolean isAllSelected;
    private boolean isTrashSelected;
    public boolean enableSelectMultiple;
    private int numMultiSelect = -1;
    private boolean isLightMode;
    private int lightColor;

    // dialog
    private boolean isNotesFiltered;

    // filter
    private FilterSheet customSheet;

    // empty list layout
    private ScrollView empty_Layout;
    private TextView title;
    private TextView subtitle;
    private TextView subSubTitle;

    public notes() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getContext();

        // initialize database and get data
        try {
            realm =  Realm.getDefaultInstance();
        } catch (Exception e) {
            realm = RealmDatabase.setUpDatabase(context);
        }
        user = AppData.getAppData().getUser(realm);
        allNotes = realm.where(Note.class)
                .equalTo("archived", false)
                .equalTo("trash", false)
                .sort("dateEditedMilli", Sort.DESCENDING).findAll();

        Log.d("Here", "All notes before size is " + allNotes.size());

        if(user.isShowFolderNotes()) {
            allNotes = allNotes.where().equalTo("category", "none").findAll();
            Log.d("Here", "All notes after size is " + allNotes.size());
        }

        Log.d("Here", "User folder is " + user.isShowFolderNotes());

        updateDateEditedMilli();

        unSelectAllNotes();

        // This callback will only be called when MyFragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(isSearchingNotes)
                    hideSearchBar();
                else
                    getActivity().finish();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_notes, container, false);

        if(user.isOpenFoldersOnStart() && AppData.isAppFirstStarted){
            AppData.isAppFirstStarted = false;
            Intent category = new Intent(getActivity(), CategoryScreen.class);
            Helper.setOrientation(getActivity(), context);
            startActivityForResult(category, 5);
        }

        if(AppData.getAppData().isLightTheme(realm)) {
            isLightMode = true;
            lightColor = context.getColor(R.color.light_mode);
            view.setBackgroundColor(lightColor);
            getActivity().getWindow().setStatusBarColor(context.getColor(R.color.light_mode));
        }

        // shows all realm notes (offline) aka notes and checklists
        initializeUi();
        initializeLayout();
        showData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Helper.unSetOrientation(getActivity(), context);

        if(user != null)
            savePreferences();

        realm = RealmDatabase.getRealm(context);

        adapterNotes.notifyDataSetChanged();
        // if list is empty, then it shows an empty layout
        isListEmpty(adapterNotes.getItemCount(), isNotesFiltered && adapterNotes.getItemCount() == 0);

        Helper.deleteCache(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(realm != null)
            realm.close();
    }

    private void setRecyclerviewLayout(){
        int span = 2;
        if(Helper.isTablet(context))
            span = 3;

        if(user.getLayoutSelected().equals("stag")) {
            StaggeredGridLayoutManager layout = new StaggeredGridLayoutManager(span, LinearLayoutManager.VERTICAL);
            layout.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
            recyclerViewNotes.setLayoutManager(layout);
        }
        else if(user.getLayoutSelected().equals("grid")){
            GridLayoutManager layout = new GridLayoutManager(context, span);
            recyclerViewNotes.setLayoutManager(layout);
        }
        else if(user.getLayoutSelected().equals("row")){
            LinearLayoutManager layout = new LinearLayoutManager(context);
            recyclerViewNotes.setLayoutManager(layout);
        }
    }

    private void initializeUi(){
        fragmentTitle = view.findViewById(R.id.fragment_title);
        sortedBy = view.findViewById(R.id.sorted_by);
        searchLayout = view.findViewById(R.id.search_padding);
        filterNotes = view.findViewById(R.id.filter);
        restoreNotes = view.findViewById(R.id.restore);
        settings = view.findViewById(R.id.settings_toolbar);
        categoryNotes = view.findViewById(R.id.category);
        searchEditText = view.findViewById(R.id.search_text);
        search = view.findViewById(R.id.search);
        recyclerViewNotes = view.findViewById(R.id.notes_recyclerview);
        empty_Layout = view.findViewById(R.id.empty_Layout);
        title = view.findViewById(R.id.empty_title);
        subtitle = view.findViewById(R.id.empty_subtitle);
        subSubTitle = view.findViewById(R.id.empty_sub_subtitle);
        filterIcon = view.findViewById(R.id.filter_icon);
        addMenu = view.findViewById(R.id.menu);
        addNote = view.findViewById(R.id.add_note);
        addCheckList = view.findViewById(R.id.add_checklist);
        adLayout = view.findViewById(R.id.ad);
        upgradeButton = view.findViewById(R.id.upgrade_button);
    }

    private void showData(){
        populateAdapter(allNotes);
        //getSortDataAndSort();
    }

    private void initializeLayout(){
        setRecyclerviewLayout();

        if(user.isProUser())
            adLayout.setVisibility(View.GONE);

        searchEditText.setIconifiedByDefault(false);
        int searchPlateId = searchEditText.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlateView = searchEditText.findViewById(searchPlateId);
        if (searchPlateView != null){
            if(isLightMode){
                searchPlateView.setBackgroundColor(getActivity().getColor(R.color.light_mode));
                int id = searchEditText.getContext()
                        .getResources()
                        .getIdentifier("android:id/search_src_text", null, null);
                TextView textView = (TextView) searchEditText.findViewById(id);
                textView.setTextColor(getActivity().getColor(R.color.black));
                ((EditText) searchEditText.findViewById(id)).setHintTextColor(context.getColor(R.color.ultra_white));
            }
            else
                searchPlateView.setBackgroundColor(getActivity().getColor(R.color.gray));
        }

        settings.setOnClickListener(v -> openSettings());

        addMenu.setOnMenuButtonClickListener(v -> {
            if(isSearchingNotes){
                hideSearchBar();
                closeMultipleNotesLayout();
                showData();
            }
            else if(isNotesFiltered || isTrashSelected || enableSelectMultiple)
                clearMultipleSelect();
            else{
                if (addMenu.isOpened())
                    addMenu.close(true);
                else
                    addMenu.open(true);
            }
        });

        filterNotes.setOnClickListener(v -> {
            if (deletingMultipleNotes)
                selectAllNotes();
            else if ((realm.where(Note.class).findAll().size() != 0)) {
                showFilterMenu();
            } else
                showMessage("Empty", "There are no notes \uD83D\uDE10", true);
        });

        categoryNotes.setOnClickListener(v -> {
           if(realm.where(Note.class).findAll().size()!=0) {
               Intent category = new Intent(getActivity(), CategoryScreen.class);
               Helper.setOrientation(getActivity(), context);
               if (enableSelectMultiple)
                   category.putExtra("multi_select", true);
               startActivityForResult(category, 5);
            }
            else
                showMessage("Empty", "There are no notes \uD83D\uDE10", true);
        });

        search.setOnClickListener(v -> {
            addMenu.close(true);
            if(deletingMultipleNotes){
                isAllSelected = false;
                deleteMultipleNotes();
            }
            else if(realm.where(Note.class).findAll().size()!=0) {
                showSearchBar();
                isListEmpty(0, true);
                populateAdapter(realm.where(Note.class).equalTo("title", "~~test~~").findAll());
            }
            else
                showMessage("Not Searching...", "Can't looking for something that does not exist", true);
        });

        restoreNotes.setOnClickListener(v -> restoreMultipleNotes());

        addNote.setOnClickListener(v -> {
            Intent note = new Intent(getActivity(), NoteEdit.class);
            startActivity(note);
            addMenu.close(true);
        });

        addCheckList.setOnClickListener(v -> {
            Intent checklist = new Intent(getActivity(), NoteEdit.class);
            checklist.putExtra("isChecklist", true);
            startActivity(checklist);
            addMenu.close(true);
        });

        searchEditText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if(isSearchingNotes) {
                    searchNotesAndUpdate(s);
                    searchingString = s;
                }
                return false;
            }
        });

        upgradeButton.setOnClickListener(view -> {
            realm.close();
            Intent settings = new Intent(context, SettingsScreen.class);
            settings.putExtra("upgrade", true);
            startActivity(settings);
            getActivity().finish();
            getActivity().overridePendingTransition(R.anim.show_from_bottom, R.anim.stay);
        });
    }

    private void updateDateEditedMilli(){
        RealmResults<Note> resultsCreated = realm.where(Note.class)
                .equalTo("dateCreatedMilli", 0)
                .findAll();

        RealmResults<Note> resultsEdited = realm.where(Note.class)
                .equalTo("dateEditedMilli", 0)
                .findAll();

        if(resultsCreated.size() != 0) {
            for (int i = 0; i < resultsCreated.size(); i++) {
                realm.beginTransaction();
                Note currentNote = resultsCreated.get(i);
                currentNote.setDateCreatedMilli(Helper.dateToCalender(currentNote.getDateCreated()).getTimeInMillis());
                realm.commitTransaction();
            }
        }

        if(resultsEdited.size() != 0) {
            for (int i = 0; i < resultsEdited.size(); i++) {
                realm.beginTransaction();
                Note currentNote = resultsEdited.get(i);
                currentNote.setDateEditedMilli(Helper.dateToCalender(currentNote.getDateEdited()).getTimeInMillis());
                realm.commitTransaction();
            }
        }

        if(resultsCreated.size() > 0 || resultsEdited.size() > 0)
            updateDateEditedMilli();
    }

    private void clearMultipleSelect(){
        numMultiSelect = -1;
        unSelectAllNotes();
        enableSelectMultiple = false;
        settings.setVisibility(View.VISIBLE);
        closeMultipleNotesLayout();
        showData();
    }

    private void openSettings(){
        realm.close();
        Intent settings = new Intent(context, SettingsScreen.class);
        startActivity(settings);
        getActivity().finish();
        getActivity().overridePendingTransition(R.anim.show_from_bottom, R.anim.stay);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 5) {
            int id = data.getIntExtra("category", -1);
            boolean viewCategoryNotes = data.getBooleanExtra("viewing", false);

            if (id != -1) {
                String currentCategory = realm.where(Folder.class).equalTo("id", id)
                        .findFirst().getName();
                RealmResults<Note> category = realm.where(Note.class)
                        .equalTo("trash", false)
                        .equalTo("archived", false)
                        .equalTo("category", currentCategory).findAll();
                filteringByCategory(category, true);
                if (!viewCategoryNotes) {
                    Helper.showMessage(getActivity(), "Added", "Note(s) have been added to " +
                            "category", MotionToast.TOAST_SUCCESS);
                    clearMultipleSelect();
                }
            }
        }
        else if (resultCode == -4 || resultCode == -2) {
            if(resultCode == -2){
                filteringByCategory(realm.where(Note.class).findAll(), true);
                restoreNotes.setVisibility(View.GONE);
            }
            else {
                closeMultipleNotesLayout();
                Helper.showMessage(getActivity(), "UnSelected", "All Selected note(s) have " +
                        "been unselected from category", MotionToast.TOAST_SUCCESS);
                clearMultipleSelect();
            }
        }
        else if (resultCode == -3) {
            restoreNotes.setVisibility(View.GONE);
            RealmResults<Note> category = realm.where(Note.class)
                    .equalTo("trash", false)
                    .equalTo("archived", false)
                    .equalTo("category", "none").findAll();
            filteringByCategory(category, true);
        }
        else if(resultCode == -5){
            RealmResults<Note> queryDeletedNotes = realm.where(Note.class).equalTo("trash", true).findAll();
            isListEmpty(queryDeletedNotes.size(), false);
            populateAdapter(queryDeletedNotes);
            if (queryDeletedNotes.size() == 0)
                isListEmpty(queryDeletedNotes.size(), true);
            isTrashSelected = true;
            restoreNotes.setVisibility(View.VISIBLE);
            settings.setVisibility(View.GONE);
            unSelectAllNotes();
            isNotesFiltered = true;
            closeFilter();
        }
        else if(resultCode == -10 || resultCode == -9 || resultCode == -8){
            RealmResults<Note> queryArchivedNotes =
                    realm.where(Note.class)
                            .equalTo("archived", true)
                            .equalTo("trash", false).findAll();

            if(resultCode == -8)
                Helper.showMessage(getActivity(), "Archived", "All Selected note(s) have " +
                        "been archived", MotionToast.TOAST_SUCCESS);
            else if(resultCode == -9)
                Helper.showMessage(getActivity(), "Un-Archived", "All Selected note(s) have " +
                        "been un-archived", MotionToast.TOAST_SUCCESS);

            if(enableSelectMultiple) {
                closeMultipleNotesLayout();
                showData();
            }
            else
                filteringAllNotesRealm(queryArchivedNotes, true);
        }
        else if(resultCode == -11 || resultCode == -12 || resultCode == -13){
            RealmResults<Note> queryArchivedNotes =
                    realm.where(Note.class)
                            .equalTo("archived", false)
                            .equalTo("pin", true)
                            .equalTo("trash", false).findAll();

            if(resultCode == -13)
                Helper.showMessage(getActivity(), "Pinned", "All Selected note(s) have " +
                        "been pinned", MotionToast.TOAST_SUCCESS);
            else if(resultCode == -12)
                Helper.showMessage(getActivity(), "Un-Pinned", "All Selected note(s) have " +
                        "been un-pinned", MotionToast.TOAST_SUCCESS);

            if(enableSelectMultiple) {
                closeMultipleNotesLayout();
                showData();
            }
            else
                filteringAllNotesRealm(queryArchivedNotes, true);
        }
        else if(resultCode == -14){
            RealmResults<Note> queryLockedNotes =
                    realm.where(Note.class)
                            .greaterThan("pinNumber", 0).findAll();

            filteringAllNotesRealm(queryLockedNotes, true);
        }
    }

    private void showFilterMenu(){
        customSheet = new FilterSheet(this);
        customSheet.show(getParentFragmentManager(), customSheet.getTag());
    }

    public void filterAndSortNotes(String dateType, boolean oldestToNewest,
                                   boolean newestToOldest, boolean aToZ, boolean zToA){
        isNotesFiltered = true;

        RealmResults<Note> result = realm.where(Note.class)
                .equalTo("archived", false)
                .equalTo("trash", false)
                .findAll();

        if(dateType!=null && !dateType.equals("null")) {
            if(newestToOldest)
                result = result.where().sort(dateType , Sort.DESCENDING).findAll();
            else if(oldestToNewest)
                result = result.where().sort(dateType , Sort.ASCENDING).findAll();
        }

        if(aToZ)
            result = result.where().sort("title").findAll();
        if(zToA)
            result = result.where().sort("title", Sort.DESCENDING).findAll();

        if(result!=null) {
            getSortDataAndSort();
            filteringAllNotesRealm(result, false);
        }
    }

    private void filteringAllNotesRealm(RealmResults<Note> query, boolean isCategory){
        isNotesFiltered = true;
        restoreNotes.setVisibility(View.GONE);
        isListEmpty(query.size(), false);
        populateAdapter(query);
        if(query.size()==0)
            isListEmpty(query.size(), true);
        if(!isCategory) {
            filterNotes.setCardBackgroundColor(context.getColor(R.color.darker_blue));
            addMenu.setMenuButtonColorNormal(context.getColor(R.color.red));
            addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.close_icon));
        }
        else
            closeFilter();
    }

    private void filteringByCategory(RealmResults<Note> query, boolean isCategory){
        isNotesFiltered = true;
        isListEmpty(query.size(), false);
        populateAdapter(query);
        if(query.size()==0)
            isListEmpty(query.size(), true);
        if(!isCategory) {
            filterNotes.setCardBackgroundColor(context.getColor(R.color.darker_blue));
            addMenu.setMenuButtonColorNormal(context.getColor(R.color.red));
            addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.close_icon));
        }
        else
            closeFilter();
    }

    public void showDefaultSort(){
        sortedBy.setVisibility(View.GONE);
        allNotes = realm.where(Note.class)
                .equalTo("archived", false)
                .equalTo("trash", false)
                .sort("dateEditedMilli", Sort.DESCENDING).findAll();
        if(user.isShowFolderNotes())
            allNotes = allNotes.where().equalTo("category", "none").findAll();
        populateAdapter(allNotes);
    }

    public void getSortDataAndSort(){
        String dateType = Helper.getPreference(getContext(), "_dateType");
        boolean oldestToNewest = Helper.getBooleanPreference(getContext(),"_oldestToNewest");
        boolean newestToOldest = Helper.getBooleanPreference(getContext(),"_newestToOldest");

        boolean aToZ = Helper.getBooleanPreference(getContext(),"_aToZ");
        boolean zToA = Helper.getBooleanPreference(getContext(),"_zToA");

        sortedBy.setVisibility(View.GONE);

        allNotes = realm.where(Note.class)
                .equalTo("archived", false)
                .equalTo("trash", false)
                .sort("dateEditedMilli", Sort.DESCENDING).findAll();

        if (dateType != null || aToZ || zToA) {
            if (oldestToNewest)
                allNotes =  allNotes.where().sort(dateType, Sort.ASCENDING).findAll();
            else if (newestToOldest)
                allNotes =  allNotes.where().sort(dateType, Sort.DESCENDING).findAll();
            else if (aToZ)
                allNotes =  allNotes.where().sort("title").findAll();
            else if (zToA)
                allNotes =  allNotes.where().sort("title", Sort.DESCENDING).findAll();
        }

        if(user.isShowFolderNotes())
            allNotes = allNotes.where().equalTo("category", "none").findAll();

        populateAdapter(allNotes);
    }

    // populates the recyclerview
    private void populateAdapter(RealmResults<Note> allNotes) {
        filteredNotes = allNotes;
        adapterNotes = new notes_recyclerview(getActivity(), notes.this,
                user.isShowPreview(), user.isShowPreviewNoteInfo());
        recyclerViewNotes.setAdapter(adapterNotes);
    }

    private void closeFilter(){
        isNotesFiltered = true;
        categoryNotes.setCardBackgroundColor(context.getColor(R.color.darker_blue));
        addMenu.setMenuButtonColorNormal(context.getColor(R.color.red));
        addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.close_icon));
    }

    private void searchNotesAndUpdate(String target){
        RealmResults<Note> queryNotes = realm.where(Note.class)
                .contains("note", target, Case.INSENSITIVE).or()
                .contains("title", target, Case.INSENSITIVE).or()
                .contains("checklistConvertedToString", target, Case.INSENSITIVE)
                .findAll();

        isListEmpty(queryNotes.size(), queryNotes.size() == 0);
        populateAdapter(queryNotes);
    }

    private void showSearchBar(){
        isSearchingNotes = true;
        fragmentTitle.setVisibility(View.GONE);
        filterNotes.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        categoryNotes.setVisibility(View.GONE);
        restoreNotes.setVisibility(View.GONE);
        searchLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        searchLayout.setCardBackgroundColor(context.getColor(R.color.gray));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0, 0);
        searchEditText.setLayoutParams(params);
        searchLayout.setPadding(0, 100, 0, 100);
        searchEditText.setVisibility(View.VISIBLE);
        searchEditText.setQueryHint("Searching...");
        searchEditText.setQuery("", false);
        searchEditText.setIconified(true);
        searchEditText.setIconified(false);

        if(isLightMode){
            searchLayout.setCardBackgroundColor(context.getColor(R.color.light_mode));
            searchEditText.setBackgroundColor(context.getColor(R.color.light_mode));
        }

        addMenu.setMenuButtonColorNormal(context.getColor(R.color.red));
        addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.back_icon));
    }

    private void hideSearchBar(){
        searchEditText.setQuery("", false);
        search.setVisibility(View.VISIBLE);
        searchLayout.setCardBackgroundColor(context.getColor(R.color.light_gray));
        fragmentTitle.setVisibility(View.VISIBLE);
        filterNotes.setVisibility(View.VISIBLE);
        searchLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        searchLayout.setCardBackgroundColor(context.getColor(R.color.gray));
        searchEditText.setVisibility(View.GONE);
        if(isTrashSelected)
            restoreNotes.setVisibility(View.VISIBLE);
        search.setVisibility(View.VISIBLE);
        categoryNotes.setVisibility(View.VISIBLE);
        searchEditText.clearFocus();

        ViewGroup.MarginLayoutParams vlp = (ViewGroup.MarginLayoutParams) filterNotes.getLayoutParams();

        LinearLayout.LayoutParams params = (new LinearLayout.LayoutParams(filterNotes.getWidth(), filterNotes.getHeight()));
        params.setMargins(0, 0, 35, 0);
        searchLayout.setLayoutParams(params);
        searchLayout.setCardBackgroundColor(context.getColor(R.color.light_gray));
        searchLayout.setPadding(filterNotes.getPaddingLeft(), filterNotes.getPaddingTop(), filterNotes.getPaddingRight(), filterNotes.getPaddingBottom());
        searchEditText.setVisibility(View.GONE);
        searchEditText.setVisibility(View.GONE);
        searchEditText.clearFocus();
        addMenu.setMenuButtonColorNormal(context.getColor(R.color.darker_blue));
        addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.add_icon));
    }

    public void deleteMultipleNotesLayout(){
        enableSelectMultiple = true;
        addMenu.setMenuButtonColorNormal(context.getColor(R.color.red));
        addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.close_icon));
        search.setImageDrawable(context.getDrawable(R.drawable.delete_icon));
        filterIcon.setImageDrawable(context.getDrawable(R.drawable.select_all_icon));
        deletingMultipleNotes = true;
    }

    public void closeMultipleNotesLayout(){
        enableSelectMultiple  = false;
        deletingMultipleNotes = false;
        unSelectAllNotes();
        clearVariables();

        restoreNotes.setVisibility(View.GONE);
        fragmentTitle.setText("Dark Note");
        fragmentTitle.setTextSize(28);
        addMenu.setMenuButtonColorNormal(context.getColor(R.color.darker_blue));
        addMenu.getMenuIconView().setImageDrawable(context.getDrawable(R.drawable.add_icon));
        search.setImageDrawable(context.getDrawable(R.drawable.search_icon));
        filterNotes.setCardBackgroundColor(context.getColor(R.color.light_gray));
        filterIcon.setImageDrawable(context.getDrawable(R.drawable.filter_icon));
        categoryNotes.setCardBackgroundColor(context.getColor(R.color.light_gray));
        settings.setVisibility(View.VISIBLE);
    }

    public void deleteMultipleNotes(){
        RealmResults<Note> selectedNotes = realm.where(Note.class).equalTo("isSelected", true).findAll();
        RealmResults<Note> lockedNotes = realm.where(Note.class).equalTo("isSelected", true)
                .greaterThan("pinNumber", 0)
                .findAll();

        if(lockedNotes.size()>0){
            Helper.showMessage(getActivity(), "Locked Noted", "Locked notes " +
                    "cannot be deleted", MotionToast.TOAST_ERROR);
        }
        else {
            if (selectedNotes.size() != 0) {
                int number = selectedNotes.size();
                if (isTrashSelected) {
                    realm.beginTransaction();
                    selectedNotes.deleteAllFromRealm();
                    realm.commitTransaction();
                    numberSelected(0, 0, 0);
                    Helper.showMessage(getActivity(), "Deleted", number + " selected " +
                            "have been deleted", MotionToast.TOAST_SUCCESS);
                    closeMultipleNotesLayout();
                    showData();
                }
                else {
                    realm.beginTransaction();
                    selectedNotes.setBoolean("trash", true);
                    realm.commitTransaction();
                    Helper.showMessage(getActivity(), "Sent to trash", number + " selected " +
                            "have been sent to trash", MotionToast.TOAST_SUCCESS);
                    numberSelected(0, 0, 0);
                    clearMultipleSelect();
                }
            }
            else
                Helper.showMessage(getActivity(), "Not Deleted", "Nothing was selected " +
                        "and thus not deleted", MotionToast.TOAST_ERROR);
            adapterNotes.notifyDataSetChanged();
        }
    }

    public void deleteNote(Note currentNote){
        if(currentNote.isTrash()){
            realm.beginTransaction();
            currentNote.deleteFromRealm();
            realm.commitTransaction();
            Helper.showMessage(getActivity(), "Deleted", "Note " +
                    "have been deleted", MotionToast.TOAST_SUCCESS);
            showData();
        }
        else{
            realm.beginTransaction();
            currentNote.setTrash(true);
            realm.commitTransaction();
            Helper.showMessage(getActivity(), "Sent to trash", "Note " +
                    "has been sent to trash", MotionToast.TOAST_SUCCESS);
        }
        adapterNotes.notifyDataSetChanged();
    }

    public void restoreMultipleNotes(){
        RealmResults<Note> selectedNotes = realm.where(Note.class).equalTo("isSelected", true)
                .equalTo("trash", true).findAll();

        if(selectedNotes.size()!=0) {
            int number = selectedNotes.size();
            realm.beginTransaction();
            selectedNotes.setBoolean("trash", false);
            realm.commitTransaction();
            adapterNotes.notifyDataSetChanged();
            Helper.showMessage(getActivity(), "Restored", number + " selected " +
                    "have been restored", MotionToast.TOAST_SUCCESS);
            clearMultipleSelect();
        }
        else
            Helper.showMessage(getActivity(), "Not Restored", "Nothing was selected " +
                    "and thus not restored", MotionToast.TOAST_ERROR);
    }

    public void numberSelected(int add, int subtract, int number){
        int currentlySelected = 1;
        if(number==-1) {
            try {
                currentlySelected = Integer.parseInt(fragmentTitle.getText().toString()
                        .replaceAll("[^0-9]", "")) + add - subtract;
            } catch (Exception e) {}
        }
        else
            currentlySelected = number;

        numMultiSelect = currentlySelected;
        fragmentTitle.setText(currentlySelected + " Selected");
        fragmentTitle.setTextSize(24);
    }

    public void unSelectAllNotes(){
        RealmResults<Note> realmResults = realm.where(Note.class).equalTo("isSelected", true).findAll();
        if(realmResults.size()!=0) {
            realm.beginTransaction();
            realmResults.setBoolean("isSelected", false);
            realm.commitTransaction();
        }
    }

    private void selectAllNotes(){
        isAllSelected = !isAllSelected;
        if(isTrashSelected){
            RealmResults<Note> realmResults = realm.where(Note.class).equalTo("trash", true).findAll();
            if (realmResults.size() != 0) {
                realm.beginTransaction();
                realmResults.setBoolean("isSelected", isAllSelected);
                realm.commitTransaction();
            }
        }
        else {
            if (filteredNotes.size() != 0) {
                realm.beginTransaction();
                filteredNotes.setBoolean("isSelected", isAllSelected);
                realm.commitTransaction();
            }
        }
        numberSelected(0, 0, isAllSelected ? adapterNotes.getItemCount() : 0);
        adapterNotes.notifyDataSetChanged();
    }

    private void clearVariables(){
        isSearchingNotes = false;
        searchEditText.setQuery("", false);
        isNotesFiltered = false;
        deletingMultipleNotes = false;
        isTrashSelected = false;
        searchingString = "";
        isAllSelected = false;
    }

    private void isListEmpty(int size, boolean isResult){
        Helper.isListEmpty(context, size, empty_Layout, title, subtitle, subSubTitle, isResult, false, false);
    }

    private void savePreferences(){
        // text size saved by user
        int savedSize = user.getTextSize();
        // text size set by user
        String textSize = Helper.getPreference(context, "size");
        int currentSize = Integer.parseInt(textSize==null ? "0" : textSize);

        // if device was backed up, then restore text size
        if(savedSize>0 && currentSize == 0) {
            Helper.savePreference(context, String.valueOf(savedSize), "size");
        }
        // if text size save
        else if(savedSize != currentSize){
            realm.beginTransaction();
            user.setTextSize(currentSize);
            realm.commitTransaction();
        }
    }

    private void showMessage(String title, String message, boolean error){
        if(error)
            Helper.showMessage(getActivity(), title, message, MotionToast.TOAST_ERROR);
        else
            Helper.showMessage(getActivity(), title, message, MotionToast.TOAST_SUCCESS);
    }
}