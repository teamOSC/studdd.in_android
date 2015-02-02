package in.tosc.studddin.fragments.signon;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.transition.Explode;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.parse.LocationCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import in.tosc.studddin.MainActivity;
import in.tosc.studddin.R;
import in.tosc.studddin.customview.MaterialEditText;
import in.tosc.studddin.externalapi.UserDataFields;
import in.tosc.studddin.utils.FutureUtils.FutureShit;
import in.tosc.studddin.utils.FutureUtils;

/**
 * SignupDataFragment
 */
public class SignupDataFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "SignupDataFragment";
    public boolean viewReady = false, bitmapReady = false;
    public Bitmap profileBitmap;
    Bundle userDataBundle;
    View rootView;
    private HashMap<String, String> input;
    private SparseArray<EditText> editTextArray = new SparseArray<>();
    private Button submitButton;
    private ImageView profileImageView;
    private GoogleApiClient mGoogleApiClient;
    private Location currentUserLoc;
    private Location approxUserLoc;

    FutureShit futureShit;

    private List<ParseObject> interests;
    private List<Integer> selectedInterests;

    public SignupDataFragment() {
        // Required empty public constructor
    }

    public static SignupDataFragment newInstance(Bundle bundle) {
        SignupDataFragment fragment = new SignupDataFragment();
        fragment.setArguments(bundle);
        return fragment;
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

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userDataBundle = getArguments();
        }
        input = new HashMap();
        connectToGoogleApi();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_sign_up, container, false);

        startLocationService();

        ParseGeoPoint.getCurrentLocationInBackground(4000, new LocationCallback() {
            @Override
            public void done(ParseGeoPoint parseGeoPoint, ParseException e) {
                if (parseGeoPoint != null) {
                    currentUserLoc = new Location("");
                    currentUserLoc.setLatitude(parseGeoPoint.getLatitude());
                    currentUserLoc.setLongitude(parseGeoPoint.getLongitude());
                }
            }
        });

        Button locationEditText = (Button) rootView.findViewById(R.id.user_location);
        locationEditText.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationSelectDialog dialog = new LocationSelectDialog();
                Bundle b = new Bundle();
                if (currentUserLoc == null) {
                    currentUserLoc = approxUserLoc;
                }
                if (currentUserLoc != null) {
                    b.putDouble("lat", currentUserLoc.getLatitude());
                    b.putDouble("lon", currentUserLoc.getLongitude());
                }

                dialog.setArguments(b);
                dialog.setLocationSetCallback(new LocationSelectDialog.LocationSetCallback() {
                    @Override
                    public void gotLocation(LatLng latLng) {
                        currentUserLoc.setLatitude(latLng.latitude);
                        currentUserLoc.setLongitude(latLng.longitude);
                    }
                });
                dialog.show(getChildFragmentManager(), "LocationSelectDialog");
            }
        });
        profileImageView = (ImageView) rootView.findViewById(R.id.sign_up_profile_picture);

        initializeEditTexts(R.id.user_name);
        initializeEditTexts(R.id.user_password);
        initializeEditTexts(R.id.user_dob);
        initializeEditTexts(R.id.user_institute);
        initializeEditTexts(R.id.user_email);
        initializeEditTexts(R.id.user_qualifications);

        final MaterialEditText interestEditText = (MaterialEditText) rootView.findViewById(R.id.user_interests);
        getInterests(interestEditText);
        futureShit.getShitDone();

        if (userDataBundle != null) {
            autoFillData();
        }

        submitButton = (Button) rootView.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInput();

                boolean f = validateInput();

                if (f) {
                    try {
                        pushInputToParse();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    startNextActivity();
                }
            }
        });

        viewReady = true;
        setProfilePicture();
        return rootView;
    }

    private String getStringFromEditText(int id) {
        try {
            return editTextArray.get(id).getText().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return " ";
        }
    }

    private void setDataToFields(int id, String fieldName) {
        try {
            editTextArray.get(id).setText(userDataBundle.getString(fieldName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void autoFillData() {
        setDataToFields(R.id.user_name, UserDataFields.USER_NAME);
        setDataToFields(R.id.user_dob, UserDataFields.USER_DOB);
        setDataToFields(R.id.user_institute, UserDataFields.USER_INSTITUTE);
        setDataToFields(R.id.user_email, UserDataFields.USER_EMAIL);
    }

    private void initializeEditTexts(int id) {
        EditText mEditText = (EditText) rootView.findViewById(id);
        if (mEditText == null) {
            Log.e(TAG, "edit text is null");
        }
        editTextArray.put(id, mEditText);
    }

    private void getInput() {
        input.put(UserDataFields.USER_NAME, getStringFromEditText(R.id.user_name));
        input.put(UserDataFields.USER_PASSWORD, getStringFromEditText(R.id.user_password));
        input.put(UserDataFields.USER_DOB, getStringFromEditText(R.id.user_dob));
        input.put(UserDataFields.USER_INSTITUTE, getStringFromEditText(R.id.user_institute));
        input.put(UserDataFields.USER_EMAIL, getStringFromEditText(R.id.user_email));
        input.put(UserDataFields.USER_INTERESTS, getStringFromEditText(R.id.user_interests));
        input.put(UserDataFields.USER_QUALIFICATIONS, getStringFromEditText(R.id.user_qualifications));
    }

    private boolean validateInput() {
        //validate input stored in input
        boolean f = true;
        if (input.get(UserDataFields.USER_NAME).isEmpty()) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.enter_name), Toast.LENGTH_LONG).show();
            f = false;
        }
        if (input.get(UserDataFields.USER_PASSWORD).isEmpty()) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.enter_name), Toast.LENGTH_LONG).show();
            f = false;
        }

        if (f && input.get(UserDataFields.USER_INSTITUTE).isEmpty()) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.enter_institute), Toast.LENGTH_LONG).show();
            f = false;
        }

        if (f && input.get(UserDataFields.USER_EMAIL).isEmpty()) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.enter_email),
                    Toast.LENGTH_LONG).show();
            f = false;
        }

        if (f && input.get(UserDataFields.USER_QUALIFICATIONS).isEmpty()) {
            Toast.makeText(getActivity(), getString(R.string.enter_qualifications),
                    Toast.LENGTH_LONG).show();
            f = false;
        }

        if (f && input.get(UserDataFields.USER_INTERESTS) == null) {
            input.remove(UserDataFields.USER_INTERESTS);
            input.put(UserDataFields.USER_INTERESTS, getActivity().getString(R.string.empty_interests));
        }

        //TODO: also make sure a valid USER_EMAIL id is entered

        return f;
    }

    private void startNextActivity() {
        //get to next activity and set flags etc in shared preferences
    }

    private void pushInputToParse() throws ParseException {
        ParseUser user = ParseUser.getCurrentUser();
        user.setUsername(input.get(UserDataFields.USER_EMAIL));
        user.setPassword(input.get(UserDataFields.USER_PASSWORD));
        user.setEmail(input.get(UserDataFields.USER_EMAIL));

        user.put(UserDataFields.USER_NAME, input.get(UserDataFields.USER_NAME));
        user.put(UserDataFields.USER_INSTITUTE, input.get(UserDataFields.USER_INSTITUTE));
        user.put(UserDataFields.USER_QUALIFICATIONS, input.get(UserDataFields.USER_QUALIFICATIONS));
//        user.put(UserDataFields.USER_INTERESTS, input.get(UserDataFields.USER_INTERESTS));
        for (int i : selectedInterests) {
            ParseObject object = interests.get(i);
            ParseRelation<ParseUser> relation = object.getRelation("users");
            relation.add(user);
            object.saveInBackground();
        }
        if (profileBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            profileBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            ParseFile profile = new ParseFile("profilePicture.png", stream.toByteArray());
            profile.save();
            user.put(UserDataFields.USER_IMAGE, profile);
        }
        if (currentUserLoc == null) {
            currentUserLoc = approxUserLoc;
        }
        if (currentUserLoc != null) {
            ParseGeoPoint geoPoint = new ParseGeoPoint(currentUserLoc.getLatitude(), currentUserLoc.getLongitude());
            user.put(UserDataFields.USER_LOCATION, geoPoint);
        }

        user.put(UserDataFields.USER_FULLY_REGISTERED, true);

        if (user.getSessionToken() != null) {
            Log.d(TAG, "saving user in background");
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        // Hooray! Let them use the app now.
                        goToMainActivity(getActivity());
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.d(TAG, "signing up user in background");
            user.signUpInBackground(new SignUpCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        // Hooray! Let them use the app now.
                        goToMainActivity(getActivity());
                    } else {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public void setProfilePicture() {
        if (viewReady && bitmapReady) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    profileImageView.setImageBitmap(profileBitmap);
                }
            });
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        currentUserLoc = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    protected synchronized void connectToGoogleApi() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void startLocationService() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                approxUserLoc = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 50, locationListener);
    }

    private void getInterests(final MaterialEditText editText) {
        editText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedInterests.add(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //TODO:: something new
            }
        });
        futureShit = new FutureShit(new Callable<List<ParseObject>>() {
            @Override
            public List<ParseObject> call() throws Exception {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Interests");
                return query.find();
            }
        }, new FutureUtils.FutureCallback<List<ParseObject>>() {
            @Override
            public void execute(List<ParseObject> result) {
                interests = result;
                List<String> interests = new ArrayList();
                for (ParseObject interest : result) {
                    interests.add(interest.getString("name"));
                }
                final ArrayAdapter<String> mAdapter = new ArrayAdapter(getActivity(),
                        android.R.layout.simple_dropdown_item_1line, interests);
                Log.d(TAG, "Got the data");
                editText.setAdapter(mAdapter);
                editText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
            }
        }
        );
    }
}
