package nwd.smile_alarm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;

public class takePhoto extends AppCompatActivity {

    private ImageView mimageView;
    private Button buttonTakePicture;
    private Button buttonProcess;
    private static int REQUEST_IMAGE_CAPTURE = 101;
    private Bitmap imageBitmap;
    AlertDialog alertDialog;
    private Ringtone r;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        // Important: have to do the following in order to show without unlocking
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        alertDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Please Wait, Processing...")
                .setCancelable(false)
                .build();

        mimageView = findViewById(R.id.imageView);
        buttonTakePicture = findViewById(R.id.button_capture);
        buttonTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
        buttonProcess = findViewById(R.id.button_process);
        buttonProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processFaceDetection(imageBitmap);
            }
        });
        r = RingtoneManager.getRingtone(getApplicationContext(),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        r.play();
        r.setLooping(true);
    }

    public void takePicture(){
        Intent imageTakeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (imageTakeIntent.resolveActivity(getPackageManager())!= null){
            startActivityForResult(imageTakeIntent,REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode==REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            mimageView.setImageBitmap(imageBitmap);
        }
    }

    private void processFaceDetection(Bitmap bitmap) {
        alertDialog.show();
        //init firebaseVisionImage
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        //init firebaseFaceDetectorOption
        FirebaseVisionFaceDetectorOptions firebaseVisionFaceDetectorOptions =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();
        //init firebaeVisionFaceDetector
        FirebaseVisionFaceDetector firebaseVisionFaceDetector =
                FirebaseVision.getInstance()
                        .getVisionFaceDetector(firebaseVisionFaceDetectorOptions);
        //let detector detect face in firebaseVisionImage and do some command
        firebaseVisionFaceDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        //scanning process
                        getFaceResults(firebaseVisionFaces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(takePhoto.this, "Error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    }
                });
    }

    private void getFaceResults(List<FirebaseVisionFace> firebaseVisionFaces) {
        for(FirebaseVisionFace face : firebaseVisionFaces){
            if (face.getSmilingProbability()!=FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                if (face.getSmilingProbability()>=0.8){
                    Intent intent = new Intent(this, WakeUp.class);
                    startActivity(intent);
                    r.stop();
                }
                else{
                    Toast.makeText(takePhoto.this, "I need your smile, please.."+face.getSmilingProbability(), Toast.LENGTH_SHORT).show();
                }
            }
            alertDialog.dismiss();
        }
    }
}
