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

public class ScheduleBusTimeHourAdapter extends RecyclerView.Adapter<ScheduleBusTimeHourAdapter.ViewHolder>{

    private int[] Hours;
    private int[][] Minutes;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView HourText;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            HourText = view.findViewById(R.id.ScheduleHourText);
        }

        public void setData(int Hour, int[] Minutes) {
            HourText.setText(Integer.toString(Hour));
        }
    }

    public ScheduleBusTimeHourAdapter(int[] Hours, int[][] Minutes) {
        this.Hours = Hours;
        this.Minutes = Minutes;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ScheduleBusTimeHourAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.schedule_bus_time_hour_recview, viewGroup, false);

        return new ScheduleBusTimeHourAdapter.ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ScheduleBusTimeHourAdapter.ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.setData(Hours[position], Minutes[position]);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return Hours.length;
    }
}
