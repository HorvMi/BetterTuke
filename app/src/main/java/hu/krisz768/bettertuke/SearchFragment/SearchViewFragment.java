package hu.krisz768.bettertuke.SearchFragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import hu.krisz768.bettertuke.Database.DatabaseManager;
import hu.krisz768.bettertuke.MainActivity;
import hu.krisz768.bettertuke.R;
import hu.krisz768.bettertuke.ScheduleFragment.ScheduleBusListAdapter;
import hu.krisz768.bettertuke.UserDatabase.Favorite;
import hu.krisz768.bettertuke.UserDatabase.UserDatabase;
import hu.krisz768.bettertuke.models.SearchResult;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchViewFragment extends Fragment {

    private static final String ALLITEM = "AllItem";

    private SearchResult[] mAllItem;

    private RecyclerView recyclerView;
    private SearchAdapter searchAdapter;

    private boolean Fav;


    public SearchViewFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static SearchViewFragment newInstance(SearchResult[] AllItem) {
        SearchViewFragment fragment = new SearchViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(ALLITEM, AllItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAllItem = (SearchResult[]) getArguments().getSerializable(ALLITEM);
        }

        Fav = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_view, container, false);

        recyclerView = view.findViewById(R.id.SearchResultRecView);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);



        searchAdapter = new SearchAdapter(GetFavoriteStops(), getContext(), this);
        recyclerView.setAdapter(searchAdapter);

        UserDatabase userDatabase = new UserDatabase(getContext());

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (target.getAdapterPosition() != 0) {
                    searchAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());

                    int FirstId = userDatabase.GetId(((SearchAdapter.ViewHolderStop)viewHolder).GetStopId(), UserDatabase.FavoriteType.Stop);
                    int SecId = userDatabase.GetId(((SearchAdapter.ViewHolderStop)target).GetStopId(), UserDatabase.FavoriteType.Stop);

                    userDatabase.SwapId(FirstId, SecId);
                    return true;
                } else {
                    return false;

                }

            }

            @Override
            public int getDragDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return viewHolder.getAdapterPosition() != 0 ? super.getDragDirs(recyclerView, viewHolder) : 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public boolean isLongPressDragEnabled() {

                return Fav;
            }
        };

        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        return view;
    }

    private SearchResult[] GetFavoriteStops() {
        UserDatabase userDatabase = new UserDatabase(getContext());

        Favorite[] favorites = userDatabase.GetFavorites(UserDatabase.FavoriteType.Stop);

        List<SearchResult> searchResultList = new ArrayList<>();

        for (int i = 0; i < favorites.length; i++) {
            searchResultList.add(new SearchResult(SearchResult.SearchType.FavStop, "", favorites[i].getData()));
        }

        SearchResult[] searchResults = new SearchResult[searchResultList.size()];
        searchResultList.toArray(searchResults);

        return searchResults;
    }

    public void OnSearchTextChanged(String text) {
        if (searchAdapter != null) {
            if (text.equals("")) {
                Fav = true;
                searchAdapter.UpdateResults(GetFavoriteStops(), true);
                searchAdapter.notifyDataSetChanged();
                return;
            }

            Fav = false;
            List<SearchResult> ResultsList = new ArrayList<>();

            for (int i = 0; i < mAllItem.length; i++) {
                if (mAllItem[i].getSearchText().toLowerCase().contains(text.toLowerCase())){
                    ResultsList.add(mAllItem[i]);
                }
            }

            SearchResult[] Results = new SearchResult[ResultsList.size()];
            ResultsList.toArray(Results);

            searchAdapter.UpdateResults(Results, false);
            searchAdapter.notifyDataSetChanged();
        }
    }

    public void OnResultClick(SearchResult searchResult) {
        if (getActivity() != null) {
            ((MainActivity)getActivity()).OnSearchResultClick(searchResult);
        }
    }
}