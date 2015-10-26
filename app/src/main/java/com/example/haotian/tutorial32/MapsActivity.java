package com.example.haotian.tutorial32;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener   {
    /* -Take Photos Simply-
    http://developer.android.com/training/camera/photobasics.html
     */

    public static final String TAG = "MapsActivity";
    public static final int THUMBNAIL = 1;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final int IMAGE_REQUEST_NUMBER = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    private String mCurrentPhotoPath;
    private String mCurrentInfoFile;
    private Marker mCurrentSelectedMarker;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates;
    private final Context context = this;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button picButton; //takes user to camera


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpDataFileIfNeeded();
        setUpMapIfNeeded();
        createLocationRequest();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        picButton = (Button) findViewById(R.id.photobutton);

        picButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // start a picture request
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        new Exception("IOException when creating image file");
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }
                }
            }
        });
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        // Setup info window click listener
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // get prompts.xml view
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.prompts, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                final EditText titleInput = (EditText) promptsView
                        .findViewById(R.id.titleTextEdit);

                final EditText snippetInput = (EditText) promptsView
                        .findViewById(R.id.snippetTextEdit);

                mCurrentSelectedMarker = marker;

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // Get user input and change the marker info
                                        mCurrentSelectedMarker.setTitle(titleInput.getText().toString());
                                        mCurrentSelectedMarker.setSnippet(snippetInput.getText().toString());
                                        mCurrentSelectedMarker.showInfoWindow();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });

        Log.i("MARKER", "Marking all previously taken pictures");

        // Read the CSV line by line
        try {
            File infoFile = new File(Environment.getExternalStoragePublicDirectory("Pictures"), mCurrentInfoFile);
            if (infoFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(infoFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    // For each line in the CSV, try to retrieve the picture that was taken
                    String[] values = line.split(",");
                    if (values[0].contains("file:")) {
                        String imagePath = values[0].substring(5);
                        File imageFile = new File(imagePath);
                        if (imageFile.exists()) {
                            // Previously taken picture exists, make a google map marker with CSV info
                            Double latitude = Double.parseDouble(values[2]);
                            Double longitude = Double.parseDouble(values[3]);

                            addMarkerFromImagePathAtLocation(imagePath, latitude, longitude);
                        } else {
                            Log.i("MARKER", "Photo no longer exists");
                        }
                    }
                }
                // Finished reading the CSV
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Places a Google Maps marker using the images within the Pictures directory at the given Lat/Long coordinates
    private void addMarkerFromImagePathAtLocation(String imagePath, double latitude, double longitude) {

        int targetW = 150;
        int targetH = 150;

        try {
            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            Bitmap bmp = BitmapFactory.decodeFile(imagePath, bmOptions);

            // Prepare a snippet for the marker
            String strCoords = "Lat: " + latitude + " - Long: " + longitude;

            // Prepare the position of the marker
            LatLng latLng = new LatLng(latitude, longitude);

            // Add marker with image as custom icon
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("New Picture")
                    .snippet(strCoords)
                    .icon(BitmapDescriptorFactory.fromBitmap(bmp)));

            Log.i("MARKER", "Placed marker at location " + latitude + ":" + longitude);
        } catch (NullPointerException e) {
            Log.e("MARKER", "Unable to retrieve existing photo with path from CSV");
        }
    }

    private void setUpDataFileIfNeeded() {
        try {
            // create the file
            mCurrentInfoFile = "Assignment3PhotoData.csv";
            File file = new File(Environment.getExternalStoragePublicDirectory("Pictures"), mCurrentInfoFile);
            if (!file.exists()) {
                file.createNewFile();

                // Create the writing stream
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fw);

                bw.write("Filename, Timestamp, Latitude, Longitude, \n");
                bw.close();

                MediaScannerConnection.scanFile(this,
                        new String[]{file.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                            }
                        });
            }

        } catch (IOException e) {
            new Exception("bad write to Assignment3Photodata");
        }


    }

    /**
     * This is all the stuff that happens when you get back from the camera app
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle the successfully taken picture
        if (requestCode == IMAGE_REQUEST_NUMBER && resultCode == RESULT_OK) {

            // scan the new picture
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            //write to the csv with the various info things
            StringBuilder sb = new StringBuilder();
            sb.append(mCurrentPhotoPath + ", ");
            //timestamp
            Long tsLong = System.currentTimeMillis() / 1000;
            sb.append(tsLong.toString() + ", ");
            //lat & long
            sb.append(mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude() +", \n");

            // Append photo info to CSV
            try {
                File infoFile = new File(Environment.getExternalStoragePublicDirectory("Pictures"), mCurrentInfoFile);
                if (infoFile.exists()) {
                    FileWriter fw = new FileWriter(infoFile, true);
                    BufferedWriter bf = new BufferedWriter(fw);
                    bf.write(sb.toString());
                    Log.i("FILEAPPEND", "Appended the new photo info to the existing file");
                    bf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Add a marker on the google map with the photo as the icon
            addMarkerFromImagePathAtLocation(mCurrentPhotoPath.substring(5),
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            Log.i("LOCATIONCHANGE","current location set: " + location.toString());
            mCurrentLocation = location;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }
}
