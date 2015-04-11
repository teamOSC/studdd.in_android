package in.tosc.studddin.fragments.signon;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Explode;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import in.tosc.studddin.MainActivity;
import in.tosc.studddin.R;
import in.tosc.studddin.externalapi.ParseTables;
import in.tosc.studddin.ui.FloatingActionButton;

/**
 * Class used for selection of Interests and College during the SignUp
 */
public class ItemSelectorFragment extends Fragment {

    private static final String TAG = "ItemSelectorFragment";

    View rootView;

    Bundle incomingBundle;

    public static final int TYPE_INTEREST = 0;
    public static final int TYPE_COLLEGE = 1;

    public static final String TYPE = "type";

    private RecyclerView itemRecyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton submitButton;

    private ItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Activity parentActivity;

    private ProgressDialog progressDialog;

    public ItemSelectorFragment() {
        // Required empty public constructor
    }

    public static ItemSelectorFragment newInstance(Bundle bundle) {
        ItemSelectorFragment fragment = new ItemSelectorFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_item_selector, container, false);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar_item_selector);
        itemRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_interests);
        submitButton = (FloatingActionButton) rootView.findViewById(R.id.button_item_submit);

        parentActivity = getActivity();
        incomingBundle = getArguments();

        final int type = incomingBundle.getInt(TYPE);

        submitButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = new ProgressDialog(parentActivity);
                progressDialog.setMessage("Saving...");
                progressDialog.show();
                pushDataToParse(type);
            }
        });

        itemRecyclerView.setHasFixedSize(true);
        mLayoutManager = new GridLayoutManager(parentActivity, 2);
        itemRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ItemAdapter(new ArrayList<ParseObject>(), type);
        itemRecyclerView.setAdapter(mAdapter);

        switch (type) {
            case TYPE_INTEREST:
                inflateInterests();
                break;
            case TYPE_COLLEGE:
                inflateColleges();
                break;
            default:
                throw new UnsupportedOperationException("Unknown ItemSelector Type");
        }
        return rootView;
    }

    private void inflateInterests() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseTables.Interests._NAME);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                showItemsList(list, TYPE_INTEREST);
            }
        });
    }

    private void inflateColleges() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseTables.College._NAME);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                showItemsList(list, TYPE_COLLEGE);
            }
        });
    }

    private void showItemsList(List<ParseObject> list, int type) {
        progressBar.setVisibility(View.GONE);
        itemRecyclerView.setVisibility(View.VISIBLE);

        mAdapter.updateDataSet(list);
        mAdapter.notifyDataSetChanged();
    }

    public void pushDataToParse(final int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SparseArray<Boolean> selectedList = mAdapter.getSelectedList();
                if (selectedList == null) {
                    throw new NullPointerException("selected list hi null hai.");
                }
                Log.d(TAG, "size of selected list - " + selectedList.size());
                List<ParseObject> mainList = mAdapter.getDataSet();
                ArrayList<ParseObject> selectedParseObjects = new ArrayList();
                ParseUser currentUser = ParseUser.getCurrentUser();
                if (currentUser != null) {
                    for (int i = 0; i < mainList.size(); ++i) {
                        if (selectedList.get(i) != null && selectedList.get(i) == true) {
                            selectedParseObjects.add(mainList.get(i));
                        }
                    }
                    if (type == TYPE_INTEREST) {
                        for (ParseObject selectedParseObject : selectedParseObjects) {
                            ParseRelation<ParseUser> relation = selectedParseObject
                                    .getRelation(ParseTables.Interests.USERS);
                            relation.add(currentUser);
                            try {
                                selectedParseObject.save();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                        currentUser.put(ParseTables.Users.INTERESTS, selectedParseObjects);
                        try {
                            currentUser.save();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        progressDialog.dismiss();
                        parentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                goToMainActivity(parentActivity);
                            }
                        });
                    }
                } else {
                    Toast.makeText(parentActivity, "You are not logged in. Please login.", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    public static void goToMainActivity(Activity act) {
        Intent i = new Intent(act, MainActivity.class);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            Activity activity = act;
            Bundle options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle();
            activity.getWindow().setExitTransition(new Explode().setDuration(1500));
            ActivityCompat.startActivityForResult(activity, i, 0, options);
        } else {
            act.startActivity(i);
        }
        act.finish();
    }

    public static class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
        private List<ParseObject> mainList;
        private SparseArray<Boolean> selectedList = new SparseArray();
        private int type;

        public class ViewHolder extends RecyclerView.ViewHolder implements
                CheckBox.OnClickListener {
            public CheckBox mCheckBox;
            public ViewHolder(LinearLayout v) {
                super(v);
                mCheckBox = (CheckBox) v.findViewById(R.id.checkbox_item);
                mCheckBox.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                Log.d(TAG, "checked position = " + getPosition());
                if (mCheckBox.isChecked()) {
                    selectedList.put(getPosition(), true);
                } else {
                    selectedList.put(getPosition(), false);
                }
            }
        }

        public ItemAdapter(List<ParseObject> myDataset, int type) {
            mainList = myDataset;
            this.type = type;
        }

        public void updateDataSet(List<ParseObject> mDataSet) {
            this.mainList = mDataSet;
        }

        @Override
        public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_item_selector, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String text = "";
            switch (type) {
                case TYPE_INTEREST:
                    text = mainList.get(position).getString(ParseTables.Interests.NAME);
                    break;
                case TYPE_COLLEGE:
                    text = mainList.get(position).getString(ParseTables.College.NAME);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown ItemSelector Type");

            }
            holder.mCheckBox.setText(text);
        }

        @Override
        public int getItemCount() {
            return mainList.size();
        }

        public SparseArray<Boolean> getSelectedList() {
            return selectedList;
        }

        public List<ParseObject> getDataSet() {
            return mainList;
        }
    }
}