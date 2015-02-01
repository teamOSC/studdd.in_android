package in.tosc.studddin.fragments.notes;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import in.tosc.studddin.R;
import in.tosc.studddin.customview.MaterialEditText;
import in.tosc.studddin.fragments.NotesFragment;
import in.tosc.studddin.utils.FloatingActionButton;
import in.tosc.studddin.utils.Utilities;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NotesSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NotesSearchFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    FloatingActionButton addNotesButton;
    EditText searchEdTxt;
    // TODO: Rename and change types of parameters
    private String mParam1;

    private String mParam2;
    private ArrayList<String> notesCollegeName, notesBranchName, notesTopicName, notesSubjectName, uploadedBy;
    private List<ParseObject> notesListParseObject;

    public NotesSearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NotesSearchFragment.
     */

    // TODO: Rename and change types and number of parameters
    public static NotesSearchFragment newInstance(String param1, String param2) {
        NotesSearchFragment fragment = new NotesSearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);

        return fragment;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().setTheme(R.style.AppTheme_Custom);
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_notes_search, container, false);

        GridView notesGridView = (GridView) rootView.findViewById(R.id.notes_gridview);

        notesCollegeName = new ArrayList<String>();
        notesBranchName = new ArrayList<String>();
        notesSubjectName = new ArrayList<String>();
        notesTopicName = new ArrayList<String>();
        uploadedBy = new ArrayList<String>();

        addNotesButton = (FloatingActionButton) rootView.findViewById(R.id.notes_button_add);
        searchEdTxt = (MaterialEditText) rootView.findViewById(R.id.notes_search);


        if (Utilities.isNetworkAvailable(getActivity())) {

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(
                    "Notes");
            query.orderByDescending("createdAt");
            try {
                notesListParseObject = query.find();
            } catch (ParseException e) {
                Log.e("Roalts", e.getMessage());
                e.printStackTrace();
            }

            for (ParseObject notes : notesListParseObject) {
                notesBranchName.add((String) notes.get("branchName"));
                notesSubjectName.add((String) notes.get("subjectName"));
                notesCollegeName.add((String) notes.get("collegeName"));
                notesTopicName.add((String) notes.get("topicName"));
                uploadedBy.add((String) notes.get("userName"));

            }
        }else
            Toast.makeText(getActivity(), "Internet Connection Problem", Toast.LENGTH_SHORT)
            .show();



        searchEdTxt = (EditText) rootView.findViewById(R.id.notes_search);


        NotesCustomGridViewAdapter adapter = new NotesCustomGridViewAdapter(getActivity(), notesCollegeName, notesBranchName, notesTopicName, notesSubjectName);

        notesGridView.setAdapter(adapter);

        notesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                NotesCustomDialog notesCustomDialog = new NotesCustomDialog(getActivity(),
                        notesCollegeName, notesBranchName, notesTopicName, notesSubjectName, position, uploadedBy);
                notesCustomDialog.setTitle(getString(R.string.notes_details));
                notesCustomDialog.show();

            }
        });



        addNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToUploadFragment();

//                String unzipLocation = "/mnt/sdcard/Notes";
//
//                NotesUnzip d = new NotesUnzip(zipFile, unzipLocation);
//                d.unzip();
            }
        });

        return rootView;

    }

    public void goToUploadFragment() {
        NotesFragment notesFragment = (NotesFragment) getParentFragment();
        if (notesFragment != null) {
            notesFragment.goToOtherFragment(1);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.notes_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.notes_search_upload:
                goToUploadFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
