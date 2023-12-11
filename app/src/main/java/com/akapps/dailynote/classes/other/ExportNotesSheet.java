package com.akapps.dailynote.classes.other;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.akapps.dailynote.R;
import com.akapps.dailynote.classes.data.Note;
import com.akapps.dailynote.classes.data.User;
import com.akapps.dailynote.classes.helpers.Helper;
import com.akapps.dailynote.classes.helpers.RealmHelper;
import com.akapps.dailynote.classes.helpers.RealmSingleton;
import com.akapps.dailynote.classes.helpers.UiHelper;
import com.akapps.dailynote.fragments.notes;
import com.deishelon.roundedbottomsheet.RoundedBottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import org.jetbrains.annotations.NotNull;

import io.realm.RealmResults;

public class ExportNotesSheet extends RoundedBottomSheetDialogFragment {

    private int numNotesSelected;
    private Fragment fragment;
    private boolean isOneNoteSelected;
    private String noteText;

    public ExportNotesSheet() {
    }

    public ExportNotesSheet(int numNotesSelected, Fragment fragment, boolean isOneNoteSelected) {
        this.numNotesSelected = numNotesSelected;
        this.fragment = fragment;
        this.isOneNoteSelected = isOneNoteSelected;
    }

    public ExportNotesSheet(int noteId, boolean isOneNoteSelected, String noteText) {
        this.numNotesSelected = noteId;
        this.isOneNoteSelected = isOneNoteSelected;
        this.noteText = noteText;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_export_notes, container, false);

        TextView numNotesSelectedText = view.findViewById(R.id.number_of_notes);
        TextView title = view.findViewById(R.id.title);
        MaterialButton exportText = view.findViewById(R.id.export_text);
        MaterialButton exportMarkdown = view.findViewById(R.id.export_markdown);
        MaterialButton exportTextString = view.findViewById(R.id.export_text_string);
        MaterialButton exportTextStringFormatted = view.findViewById(R.id.export_text_string_formatted);
        MaterialButton exportNote = view.findViewById(R.id.export_note);
        ImageButton info = view.findViewById(R.id.export_info);

        if (RealmHelper.getUser(getContext(), "bottom sheet").getScreenMode() == User.Mode.Dark) {
            view.setBackgroundColor(getContext().getColor(R.color.black));
            exportText.setBackgroundColor(getContext().getColor(R.color.black));
            exportMarkdown.setBackgroundColor(getContext().getColor(R.color.black));
            exportTextString.setBackgroundColor(getContext().getColor(R.color.black));
            exportTextStringFormatted.setBackgroundColor(getContext().getColor(R.color.black));
            exportNote.setBackgroundColor(getContext().getColor(R.color.black));
            exportText.setStrokeWidth(5);
            exportMarkdown.setStrokeWidth(5);
            exportTextString.setStrokeWidth(5);
            exportTextStringFormatted.setStrokeWidth(5);
            exportNote.setStrokeWidth(5);
            exportText.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.blue)));
            exportMarkdown.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.azure)));
            exportTextString.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.golden_rod)));
            exportTextStringFormatted.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.gumbo)));
            exportNote.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.money_green)));
        } else if (RealmHelper.getUser(getContext(), "bottom sheet").getScreenMode() == User.Mode.Gray) {
            view.setBackgroundColor(getContext().getColor(R.color.gray));
            exportTextString.setTextColor(getContext().getColor(R.color.gray));
        } else if (RealmHelper.getUser(getContext(), "bottom sheet").getScreenMode() == User.Mode.Light) {

        }

        if (!isOneNoteSelected) {
            exportTextString.setVisibility(View.GONE);
            exportTextStringFormatted.setVisibility(View.GONE);
            exportNote.setVisibility(View.GONE);
            numNotesSelectedText.setText("" + numNotesSelected);
            info.setVisibility(View.GONE);
        } else
            title.setText("Export Note");

        exportText.setOnClickListener(view12 -> {
            if (isOneNoteSelected)
                exportNote(".txt");
            else
                exportNotes(".txt");
        });

        exportMarkdown.setOnClickListener(view1 -> {
            if (isOneNoteSelected)
                exportNote(".md");
            else
                exportNotes(".md");
        });

        exportTextString.setOnClickListener(view14 -> {
            String removeFormatting = Helper.removeMarkdownFormatting(noteText);
            Helper.shareFile(getActivity(), removeFormatting);
            dismiss();
        });

        exportTextStringFormatted.setOnClickListener(view13 -> {
            Helper.shareFile(getActivity(), noteText);
            dismiss();
        });

        exportNote.setOnClickListener(view15 -> {
            Helper.shareNote(getActivity(), numNotesSelected, RealmSingleton.getInstance(getContext()));
            dismiss();
        });

        info.setOnClickListener(view16 -> {

            String exportInfo = "" +
                    "• Text file\n" +
                    "  - export note as text file\n\n" +
                    "• Markdown file\n" +
                    "  - export note as markdown file\n\n" +
                    "• Text\n" +
                    "  - export note as text (removes all formatting)\n\n" +
                    "• Text (Markdown)\n" +
                    "  - export note as markdown text (keeps all formatting)\n\n" +
                    "• Export Note\n" +
                    "  - exports note as text\n" +
                    "  - exports all images and audio files";

            InfoSheet info1 = new InfoSheet(exportInfo, 12);
            info1.show(getActivity().getSupportFragmentManager(), info1.getTag());
        });

        return view;
    }

    private void exportNotes(String extension) {
        RealmResults<Note> results = Helper.getSelectedNotes(RealmSingleton.getInstance(getContext()),
                getActivity());
        if (results != null && results.size() != 0)
            Helper.exportFiles(extension, getActivity(), results, RealmSingleton.getInstance(getContext()));
        ((notes) fragment).clearMultipleSelect();
        dismiss();
    }

    private void exportNote(String extension) {
        Helper.exportFiles(extension, getActivity(), RealmSingleton.getInstance(getContext()).where(Note.class)
                .equalTo("noteId", numNotesSelected).findAll(), RealmSingleton.getInstance(getContext()));
        dismiss();
    }

    @Override
    public int getTheme() {
        return UiHelper.getBottomSheetTheme(getContext());
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        UiHelper.setBottomSheetBehavior(view, dialog);
    }

}