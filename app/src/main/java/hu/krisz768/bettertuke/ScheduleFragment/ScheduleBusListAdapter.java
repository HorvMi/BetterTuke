package hu.krisz768.bettertuke.ScheduleFragment;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import hu.krisz768.bettertuke.Database.BusLine;
import hu.krisz768.bettertuke.R;
import hu.krisz768.bettertuke.UserDatabase.Favorite;

public class ScheduleBusListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private BusLine[] BusLines;
    private BusLine[] Favorites;
    private Context ctx;
    private ScheduleBusListFragment Callback;

    public static class ViewHolderLine extends RecyclerView.ViewHolder {

        private TextView Number;
        private TextView Description;
        private View view;
        private BusLine busLine;

        public ViewHolderLine(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            Number = view.findViewById(R.id.ScheduleLineNum);
            Description = view.findViewById(R.id.SearchBusStopName);
            this.view = view;
        }

        public void setData(BusLine busLine, Context ctx, ScheduleBusListFragment Callback) {
            this.busLine = busLine;

            TypedValue typedValue = new TypedValue();
            ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);

            Number.setTextColor(ContextCompat.getColor(ctx, typedValue.resourceId));
            Number.setText(busLine.getLineName());
            Description.setText(busLine.getLineDesc());

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Callback.OnLineClick(busLine.getLineName());
                }
            });
        }

        public String GetName() {
            return busLine.getLineName();
        }
    }

    public static class ViewHolderLabel extends RecyclerView.ViewHolder {

        private TextView Label;

        public ViewHolderLabel(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            Label = view.findViewById(R.id.labelText);
        }

        public void setData(String LabeText) {
            Label.setText(LabeText);
        }
    }

    public ScheduleBusListAdapter(BusLine[] BusLines, BusLine[] Favorites, Context ctx, ScheduleBusListFragment Callback) {
        this.BusLines = BusLines;
        this.ctx = ctx;
        this.Callback = Callback;
        this.Favorites = Favorites;
    }

    @Override
    public int getItemViewType(int position) {
        if (Favorites.length == 0) {
            return 0;
        } else {
            if (position == 0 || position == Favorites.length+1) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.schedule_line_list_recview, viewGroup, false);

            return new ScheduleBusListAdapter.ViewHolderLine(view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.recview_label, viewGroup, false);

            return new ScheduleBusListAdapter.ViewHolderLabel(view);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {

        if (Favorites.length == 0) {
            ((ViewHolderLine)viewHolder).setData(BusLines[position], ctx, Callback);
        } else {
            if (position == 0 || position == Favorites.length+1) {
                if (position == 0) {
                    ((ViewHolderLabel)viewHolder).setData("Kedvencek:");
                } else {
                    ((ViewHolderLabel)viewHolder).setData("Összes:");
                }
            } else {
                if (position < Favorites.length+1) {
                    ((ViewHolderLine)viewHolder).setData(Favorites[position-1], ctx, Callback);
                } else {
                    ((ViewHolderLine)viewHolder).setData(BusLines[position-Favorites.length-2], ctx, Callback);
                }
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (Favorites.length == 0) {
            return BusLines.length;
        } else {
            return BusLines.length + Favorites.length + 2;
        }

    }
}
