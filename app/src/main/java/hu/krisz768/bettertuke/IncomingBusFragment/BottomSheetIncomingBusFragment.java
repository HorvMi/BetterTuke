package hu.krisz768.bettertuke.IncomingBusFragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hu.krisz768.bettertuke.Database.BusJaratok;
import hu.krisz768.bettertuke.Database.BusPlaces;
import hu.krisz768.bettertuke.Database.BusStops;
import hu.krisz768.bettertuke.HelperProvider;
import hu.krisz768.bettertuke.InfoFragment;
import hu.krisz768.bettertuke.LoadingFragment;
import hu.krisz768.bettertuke.MainActivity;
import hu.krisz768.bettertuke.R;
import hu.krisz768.bettertuke.UserDatabase.UserDatabase;
import hu.krisz768.bettertuke.api_interface.TukeServerApi;
import hu.krisz768.bettertuke.api_interface.models.IncommingBusRespModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BottomSheetIncomingBusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BottomSheetIncomingBusFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String PLACE = "Place";
    private static final String STOP = "Stop";
    private static final String PLACELIST = "PlaceList";
    private static final String STOPLIST = "StopList";

    private int mPlace;
    private volatile int mStop;
    private BusPlaces[] mPlaceList;
    private BusStops[] mStopList;

    private IncomingBusStopSelectorAdapter Ibssa;
    private IncomingBusListFragment InBusFragment;
    private Thread UpdateThread;
    private volatile boolean UpdateThreadRun = true;

    public BottomSheetIncomingBusFragment() {
        // Required empty public constructor
    }
    public static BottomSheetIncomingBusFragment newInstance(int Place, int Stop, BusPlaces[] PlaceList, BusStops[] StopList) {
        BottomSheetIncomingBusFragment fragment = new BottomSheetIncomingBusFragment();
        Bundle args = new Bundle();
        args.putInt(PLACE, Place);
        args.putInt(STOP, Stop);
        args.putSerializable(PLACELIST, PlaceList);
        args.putSerializable(STOPLIST, StopList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPlace = getArguments().getInt(PLACE);
            mStop = getArguments().getInt(STOP);
            mPlaceList = (BusPlaces[]) getArguments().getSerializable(PLACELIST);
            mStopList = (BusStops[]) getArguments().getSerializable(STOPLIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bottom_sheet_incoming_bus_view, container, false);

        TextView Teszt = view.findViewById(R.id.BusStopName);

        ImageView ScheduleButton = view.findViewById(R.id.StopScheduleButton);
        ScheduleButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapStopSelected));
        ScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)getActivity()).ShowSchedule(mStop, null, null, null, false);
            }
        });

        ImageView FavButton = view.findViewById(R.id.StopFavoriteButton);

        UserDatabase userDatabase = new UserDatabase(getContext());

        if (userDatabase.IsFavorite(UserDatabase.FavoriteType.Stop, Integer.toString(mStop))) {
            FavButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.FaviconOn));
        } else {
            FavButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.FaviconOff));
        }

        FavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userDatabase.IsFavorite(UserDatabase.FavoriteType.Stop, Integer.toString(mStop))) {
                    userDatabase.DeleteFavorite(UserDatabase.FavoriteType.Stop, Integer.toString(mStop));
                    FavButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.FaviconOff));
                } else {
                    userDatabase.AddFavorite(UserDatabase.FavoriteType.Stop, Integer.toString(mStop));
                    FavButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.FaviconOn));
                }
            }
        });


        List<BusStops> SelectedPlaceStops = new ArrayList<>();

        for (int i = 0; i < mPlaceList.length; i++) {
            if (mPlaceList[i].getId() == mPlace) {
                Teszt.setText(mPlaceList[i].getName());

                for (int j = 0; j < mStopList.length; j++) {
                    if (mStopList[j].getFoldhely() == mPlaceList[i].getId()) {
                        SelectedPlaceStops.add(mStopList[j]);
                    }
                }
            }
        }

        BusStops[] SelectedPlaceStopsArray = new BusStops[SelectedPlaceStops.size()];
        SelectedPlaceStops.toArray(SelectedPlaceStopsArray);

        Ibssa = new IncomingBusStopSelectorAdapter(SelectedPlaceStopsArray,mStop, this,getContext());

        RecyclerView StopSelectorRec = view.findViewById(R.id.BusStopListRecView);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        StopSelectorRec.setLayoutManager(mLayoutManager);
        StopSelectorRec.setAdapter(Ibssa);

        StopSelectorRec.setNestedScrollingEnabled(false);

        StartNewUpdateThread(view);

        return view;
    }

    private void StartNewUpdateThread(View view) {

        UpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                GetIncommingBusesLoop(view);
            }
        });
        UpdateThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        UpdateThreadRun = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        ResetList();
        UpdateThreadRun = true;
    }

    public void OnStopClick(int Id) {
        ((MainActivity)getActivity()).ChangeStop(Id);
        mStop = Id;
        Ibssa.setSelectedStop(mStop);
        Ibssa.notifyDataSetChanged();
        ResetList();

        ImageView FavButton = getView().findViewById(R.id.StopFavoriteButton);

        UserDatabase userDatabase = new UserDatabase(getContext());
        if (userDatabase.IsFavorite(UserDatabase.FavoriteType.Stop, Integer.toString(mStop))) {
            FavButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.FaviconOn));
        } else {
            FavButton.setImageBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.FaviconOff));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                TukeServerApi serverApi = new TukeServerApi(getActivity());
                GetIncommingBuses(serverApi);
            }
        }).start();

    }

    private void ResetList() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.BusListFragment, new LoadingFragment())
                .commit();
        InBusFragment = null;
    }

    private void GetIncommingBusesLoop(View view) {
            TukeServerApi serverApi = new TukeServerApi(this.getActivity());

            try {
                while (true) {
                    if (getContext() == null)
                        break;
                    if (!UpdateThreadRun) {
                        Thread.sleep(1000);
                        continue;
                    }
                    GetIncommingBuses(serverApi);

                    for (int i = 0; i < 20; i++) {
                        Thread.sleep(500);
                        if (!UpdateThreadRun)
                            break;
                    }
                }
            } catch (Exception e) {

            }
    }

    private void GetIncommingBuses(TukeServerApi serverApi) {
        try {
            final int SendStopid = mStop;
            Log.i("Update", "Updating List");
            IncommingBusRespModel[] BusList = serverApi.getNextIncommingBuses(mStop);

            Date currentTime = Calendar.getInstance().getTime();
            SimpleDateFormat Sdf = new SimpleDateFormat("H", Locale.US);
            SimpleDateFormat Sdf2 = new SimpleDateFormat("m", Locale.US);

            for (int i = 0; i < BusList.length; i++) {
                BusJaratok Bj = BusJaratok.BusJaratokByJaratid(BusList[i].getJaratid(), getContext());
                if (Bj.getIndulasOra() < Integer.parseInt(Sdf.format(currentTime)) || (Bj.getIndulasOra() == Integer.parseInt(Sdf.format(currentTime)) && Bj.getIndulasPerc() <= Integer.parseInt(Sdf2.format(currentTime)))) {
                    BusList[i].setElindult(serverApi.getIsBusHasStarted(BusList[i].getJaratid()));
                    //Log.e("ASDASDASDASD", Bj.getIndulasOra() + ":" + Bj.getIndulasPerc() + " " + Integer.parseInt(Sdf.format(currentTime))+ ":" + Integer.parseInt(Sdf2.format(currentTime)));
                } else {
                    BusList[i].setElindult(false);
                }

            }

            if(SendStopid != mStop) {
                return;
            }

            if (BusList.length > 0) {
                if (InBusFragment == null) {

                    InBusFragment = IncomingBusListFragment.newInstance(BusList);
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.BusListFragment, InBusFragment)
                            .commit();
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (InBusFragment != null) {
                                InBusFragment.UpdateList(BusList);
                            }

                        }
                    });
                }
            } else {
                InBusFragment = null;

                InfoFragment Ifragment = InfoFragment.newInstance(getResources().getString(R.string.EmptyList), -1);
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.BusListFragment, Ifragment)
                        .commit();
            }
        } catch (Exception e) {
            Log.e("Update bus list error", e.toString());
            //e.printStackTrace();
        }
    }
}