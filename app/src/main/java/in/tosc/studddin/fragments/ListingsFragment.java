package in.tosc.studddin.fragments;


import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;

import in.tosc.studddin.R;
import in.tosc.studddin.fragments.listings.ListingsSearchFragment;
import in.tosc.studddin.fragments.listings.ListingsUploadFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListingsFragment extends Fragment {


    ViewPager notesPager;
    FragmentPagerAdapter fragmentPagerAdapter;

    public ListingsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_notes, container, false);

        fragmentPagerAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0: return (new ListingsSearchFragment());
                    case 1: return (new ListingsUploadFragment());
                }
                return new ListingsSearchFragment();
            }

            @Override
            public int getCount() {
                return 2;
            }
        };

        notesPager = (ViewPager) rootView.findViewById(R.id.notes_pager);
        notesPager.setAdapter(fragmentPagerAdapter);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        Log.d("onActivityResult", "call ho raha hai" + resultCode + "   " + requestCode);
        if ((requestCode == 0) && resultCode == -1) {
            if (data == null) {
                Log.d("onActivityResult","camera");
                compressImage(ListingsUploadFragment.mCurrentPhotoPath);
            } else {
                Log.d("onActivityResult","docs");
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getActivity().getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                compressImage(picturePath);
            }
        }
    }

    private void compressImage(String path){
        int targetW = ListingsUploadFragment.listing_image.getWidth();
        int targetH = ListingsUploadFragment.listing_image.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
        ListingsUploadFragment.listing_image.setImageBitmap(bitmap);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream);
        ListingsUploadFragment.byteArray = stream.toByteArray();
    }
}
