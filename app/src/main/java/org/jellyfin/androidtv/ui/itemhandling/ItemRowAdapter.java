package org.jellyfin.androidtv.ui.itemhandling;

import androidx.leanback.widget.Presenter;

import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;

import java.util.List;

public class ItemRowAdapter extends MutableObjectAdapter<Object> {
    private List<ChapterItemInfo> mChapters;

    public ItemRowAdapter(List<ChapterItemInfo> chapters, Presenter presenter) {
        super(presenter);
        mChapters = chapters;
    }

    public void Retrieve() {
        if (mChapters != null) {
            for (ChapterItemInfo chapter : mChapters) {
                add(new ChapterItemInfoBaseRowItem(chapter));
            }
        }
    }
}
