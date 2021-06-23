package com.asiczen.azlock.util;

import android.graphics.Canvas;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import com.asiczen.azlock.Adapters.RecyclerAdapters;


public class RecyclerItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private final RecyclerItemTouchHelperListener listener;
    public RecyclerItemTouchHelper(int dragDirs, int swipeDirs, RecyclerItemTouchHelperListener listener) {
        super(dragDirs, swipeDirs);
        this.listener = listener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (listener != null){
            listener.onSwipe(viewHolder,direction,viewHolder.getAdapterPosition());
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        View background = ((RecyclerAdapters.ViewHolder)viewHolder).view_background;
        //foregroundView.setVisibility(View.VISIBLE);
        getDefaultUIUtil().clearView(background);
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }


    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if(viewHolder != null){
            View background = ((RecyclerAdapters.ViewHolder)viewHolder).view_background;
            getDefaultUIUtil().onSelected(background);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View background = ((RecyclerAdapters.ViewHolder)viewHolder).view_background;
        getDefaultUIUtil().onDraw(c,recyclerView,background,dX,dY,actionState,isCurrentlyActive);
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View background = ((RecyclerAdapters.ViewHolder)viewHolder).view_background;
        getDefaultUIUtil().onDraw(c,recyclerView,background,dX,dY,actionState,isCurrentlyActive);
    }
}
