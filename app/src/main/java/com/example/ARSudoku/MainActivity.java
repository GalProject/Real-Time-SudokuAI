package com.example.ARSudoku;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;


import android.os.Environment;
import android.util.Log;

import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.core.Size;
import org.w3c.dom.Element;
import com.googlecode.tesseract.android.TessBaseAPI;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private Mat mRGBa;
    private Mat mIntermediateMat;
    private Mat mGray;
    Mat hierarchy;
    List<MatOfPoint> contours;
    private CameraBridgeViewBase mOpenCvCameraView;



    TessBaseAPI tessBaseApi;
    public boolean isSolved = false;
    int isWritten = 0;
    public int[][] Subox = new int[9][9];
    public int x,y;
    public static final String lang = "eng";
    ContextWrapper c = new ContextWrapper(this);
    public String DATA_PATH;

    boolean slvBtn = false;
    int[][] OnlySolved = new int[9][9];


    Point TopLeft;
    Point TopRight;
    Point BotLeft;
    Point BotRight;
    Button solveButton;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }


        /*

            <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
            <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
            <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }*/

        /*if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }*/



        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);

        File directory = new File(c.getFilesDir(), "/Directory/tessdata/");
        if (!directory.exists()) {

            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + directory.toString() + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + directory.toString() + " on sdcard");

                }
            }

        }
        else
        {
            Log.v(TAG, "ERROR: Directory "+directory.toString()+" Exist");
        }
        DATA_PATH = c.getFilesDir().toString() + "/Directory";


        if (!(new File(DATA_PATH  + "/tessdata/eng.traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open(  "tessdata/eng.traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "/tessdata/eng.traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }


        solveButton = findViewById(R.id.solveButton);//get id of button 1

        solveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slvBtn = true;
            }
        });

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    public void onCameraViewStarted(int width, int height) {
        mRGBa = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
        hierarchy = new Mat();


    }

    public void onCameraViewStopped() {
        mRGBa.release();
        mGray.release();
        mIntermediateMat.release();
        hierarchy.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRGBa = inputFrame.rgba();
        Mat mGray = new Mat();
        Imgproc.cvtColor(mRGBa,mGray,Imgproc.COLOR_RGB2GRAY);
        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(mGray, blurMat, new Size(9, 9), 0);
        Mat threshMat = new Mat();
        Imgproc.adaptiveThreshold(blurMat, threshMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
        //Core.bitwise_not(threshMat,threshMat);
        //Imgproc.dilate(threshMat,threshMat, new Mat());

        contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(threshMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        MatOfPoint2f biggest = new MatOfPoint2f();
        double maxArea = 0;
        for (MatOfPoint i : contours) {
            double area = Imgproc.contourArea(i);
            if (area > 100) {
                MatOfPoint2f k = new MatOfPoint2f(i.toArray());
                double peri = Imgproc.arcLength(k, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(k, approx, 0.02 * peri, true);
                if (area > maxArea && approx.total() == 4) {
                    biggest = approx;
                    maxArea = area;
                }
            }
        }



        Point[] points = biggest.toArray();
        if (points.length >= 4 && maxArea > 3000 ) {
            Imgproc.line(mRGBa, new Point(points[0].x, points[0].y), new Point(points[1].x, points[1].y), new Scalar(0, 255, 0, 0), 4);
            Imgproc.line(mRGBa, new Point(points[1].x, points[1].y), new Point(points[2].x, points[2].y), new Scalar(0, 255, 0, 0), 4);
            Imgproc.line(mRGBa, new Point(points[2].x, points[2].y), new Point(points[3].x, points[3].y), new Scalar(0, 255, 0, 0), 4);
            Imgproc.line(mRGBa, new Point(points[3].x, points[3].y), new Point(points[0].x, points[0].y), new Scalar(0, 255, 0, 0), 4);


        }

        if (points.length >= 4 && maxArea > 3000 ) {


            TopLeft = new Point(points[0].x, points[0].y);
            TopRight = new Point(points[0].x, points[0].y);
            BotLeft = new Point(points[0].x, points[0].y);
            BotRight = new Point(points[0].x, points[0].y);


            double min = (points[0].x + points[0].y);
            double maxY = points[0].y;
            double maxX = points[0].x;
            double max = (points[0].x + points[0].y);
            for (Point i : points) {
                if (i.x + i.y < min) {
                    min = i.x + i.y;
                    TopLeft = i;

                }

                if (i.x + i.y > max) {
                    BotRight = i;
                    max = i.x + i.y;
                }

                if (i.y > maxY) {
                    maxY = i.y;
                }

                if (i.x > maxX) {
                    maxX = i.x;
                }


            }
            min = (points[0].x + maxY - points[0].y);
            for (Point i : points) {
                if (i.x + maxY - i.y < min) {
                    BotLeft = i;
                    min = i.x + maxY - i.y;
                }

                if (i != TopLeft && i != BotLeft && i != BotRight) {
                    TopRight = i;
                }

            }


            if(slvBtn)
            {

            /* For debug purpose,
            Imgproc.putText(mRGBa, "TL", TopLeft, 1, 3.0f, new Scalar(0, 255, 0, 0), 4);
            Imgproc.putText(mRGBa, "BL", BotLeft, 1, 3.0f, new Scalar(0, 255, 0, 0), 4);
            Imgproc.putText(mRGBa, "BR", BotRight, 1, 3.0f, new Scalar(0, 255, 0, 0), 4);
            Imgproc.putText(mRGBa, "TR", TopRight, 1, 3.0f, new Scalar(0, 255, 0, 0), 4);

            */
            double width_A = Math.sqrt(Math.pow(BotRight.x - BotLeft.x, 2) + Math.pow(BotRight.y - BotLeft.y, 2));
            double width_B = Math.sqrt(Math.pow(TopRight.x - TopLeft.x, 2) + Math.pow(TopRight.y - TopLeft.y, 2));
            double height_A = Math.sqrt(Math.pow(TopLeft.x - BotLeft.x, 2) + Math.pow(TopLeft.y - BotLeft.y, 2));
            double height_B = Math.sqrt(Math.pow(TopRight.x - BotRight.x, 2) + Math.pow(TopRight.y - BotRight.y, 2));


            double height = Math.max(height_A, height_B);
            double width = Math.max(width_A, width_B);

            if (height > 100 && width > 100 ) {


                /*if(isWritten == 0)
                {
                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    String filename = "test1.png";
                    File file = new File(path,filename);
                    filename = file.toString();
                    Imgcodecs.imwrite(filename, cropped);
                    isWritten = 1;
                }*/


                tessBaseApi = new TessBaseAPI();
                tessBaseApi.init(DATA_PATH, "eng");
                tessBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
                tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789");
                tessBaseApi.setVariable("classify_bin_numeric_mode", "1");

                //Mat cropped2 = cropped.clone();
                Point[] sortedPoints = {TopLeft,TopRight,BotLeft,BotRight};

                Mat cropped2 = PerspectiveTransform(sortedPoints,height,width, mRGBa);


                double HeightCheck = cropped2.height() * 0.9;
                double WidthCheck = cropped2.width() * 0.9;
                Rect cropRec = cropBiggestSquare(cropped2);

                if(cropRec.area() > HeightCheck * WidthCheck)
                    cropped2 = new Mat(cropped2,cropRec);

                /*if(isWritten == 0)
                {
                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    String filename = "test1.png";
                    File file = new File(path,filename);
                    filename = file.toString();
                    Imgcodecs.imwrite(filename, cropped2);
                    isWritten = 1;
                }*/

                if (cropped2.width() > 1 && cropped2.height() > 1) {

                    double Img_Width = cropped2.width();
                    double Img_Height = cropped2.height();
                    double Height_Step = Img_Height / 9;
                    double Width_Step = Img_Width / 9;
                    double HPadding = Img_Height / 27;
                    double WPadding = Img_Width / 26;

                    for (y = 0; y <= 8; y++) {
                        for (x = 0; x <= 8; x++) {
                            Subox[y][x] = 0;
                        }
                    }

                    int iy, ix;
                    for (y = 0, iy = 0; y < Img_Height && iy <= 8; y += Height_Step, iy++) {
                        for (x = 0, ix = 0; x < Img_Width && ix <= 8; x += Width_Step, ix++) {
                            Subox[iy][ix] = 0;
                            double cx = (x + Width_Step / 2);
                            double cy = (y + Height_Step / 2);
                            Point p1 = new Point(cx - WPadding, cy - HPadding);
                            Point p2 = new Point(cx + WPadding, cy + HPadding);
                            Rect Re = new Rect(p1, p2);
                            Mat digitCrop = new Mat(cropped2,Re);
                            Imgproc.cvtColor(digitCrop,digitCrop,Imgproc.COLOR_RGB2GRAY);
                            //Imgproc.GaussianBlur(digitCrop, digitCrop, new Size(5, 5), 0);w
                            //Imgproc.adaptiveThreshold(digitCrop, digitCrop, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 10);
                            Imgproc.GaussianBlur(digitCrop, digitCrop, new Size(5, 5), 0);

                            Imgproc.adaptiveThreshold(digitCrop, digitCrop, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 10);

                            ;
                            //Imgproc.GaussianBlur(digitCrop, digitCrop, new Size(5, 5), 0);

                            //Imgproc.adaptiveThreshold(digitCrop, digitCrop, 255, 1, 1, 11, 2);
                            //Imgproc.floodFill(digitCrop,new Mat(),new Point(0,0),new Scalar(255,255,255));
                            /*contours = new ArrayList<>();

                            hierarchy = new Mat();
                            Imgproc.findContours(digitCrop, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                            hierarchy.release();

                            biggest = new MatOfPoint2f();
                            maxArea = 0;

                            for (MatOfPoint i : contours) {
                                double area = Imgproc.contourArea(i);
                                if (area > 10) {
                                    MatOfPoint2f k = new MatOfPoint2f(i.toArray());
                                    double peri = Imgproc.arcLength(k, true);
                                    MatOfPoint2f approx = new MatOfPoint2f();
                                    Imgproc.approxPolyDP(k, approx, 0.02 * peri, true);
                                    if (area > maxArea && approx.total() == 4) {
                                        biggest = approx;
                                        maxArea = area;
                                    }
                                }
                            }

                           /* points = biggest.toArray();
                            if (points.length >= 4 & maxArea > 1000) {
                                Imgproc.line(digitCrop, new Point(points[0].x, points[0].y), new Point(points[1].x, points[1].y), new Scalar(0, 255, 0, 0), 4);
                                Imgproc.line(digitCrop, new Point(points[1].x, points[1].y), new Point(points[2].x, points[2].y), new Scalar(0, 255, 0, 0), 4);
                                Imgproc.line(digitCrop, new Point(points[2].x, points[2].y), new Point(points[3].x, points[3].y), new Scalar(0, 255, 0, 0), 4);
                                Imgproc.line(digitCrop, new Point(points[3].x, points[3].y), new Point(points[0].x, points[0].y), new Scalar(0, 255, 0, 0), 4);


                            }*/
                            /*
                                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                                String filename = "test" +ix+""+iy+ ".png";
                                File file = new File(path,filename);
                                filename = file.toString();
                                Imgcodecs.imwrite(filename, digitCrop);
                               */


                            //Imgproc.rectangle(cropped2, p1, p2, new Scalar(0, 0, 0));
                            if (digitCrop.height() > 1 && digitCrop.width() > 1) {
                                //Bitmap digit_bitmap = Bitmap.createBitmap(digitCrop.cols(), digitCrop.rows(),Bitmap.Config.ARGB_8888);
                                Bitmap digit_bitmap = Bitmap.createBitmap(digitCrop.cols(), digitCrop.rows(), Bitmap.Config.ARGB_8888);
                                Utils.matToBitmap(digitCrop, digit_bitmap);
                                //digit_bitmap =  rotateBitmap(digit_bitmap,-90);
                                tessBaseApi.setImage(digit_bitmap);
                                String recognizedText = tessBaseApi.getUTF8Text();
                                if (recognizedText.length() == 1) {
                                    Subox[iy][ix] = Integer.parseInt(recognizedText);
                                }
                                tessBaseApi.clear();
                            }
                        }

                        /*tessBaseApi.getUTF8Text();*/
                       /* if (recognizedText.length() == 1) {
                            Subox[iy][ix] = Integer.valueOf(recognizedText);
                        / roi.y, roi.y + roi.height, roi.x, roi.x + roi.width
                        }*/


                    }

                }
                tessBaseApi.clear();

                /*if (Subox[0][0] != 0)
                    Imgproc.putText(mRGBa, Subox[0][0] + "", new Point(TopLeft.x, TopLeft.y),
                        1, 3.0f, new Scalar(0, 255, 0, 0), 4);
*/
                //PrintBoard(mRGBa,Subox,TopLeft,HeightStep,WidthStep);

                int[][] UnsolvedBoard = new int[9][9];
                OnlySolved = new int[9][9];

                for (int i = 0; i <= 8; i++) {
                    for (int j = 0; j <= 8; j++) {
                        UnsolvedBoard[i][j] = Subox[i][j];
                    }
                }

                Solver solver = new Solver(Subox);
                int[][] SolvedBox = solver.mainSolver();


                for (int i = 0; i <= 8; i++) {
                    for (int j = 0; j <= 8; j++) {
                        if (UnsolvedBoard[i][j] == 0) {
                            OnlySolved[i][j] = SolvedBox[i][j];
                        }
                    }
                }

                if (notEmpty(OnlySolved) && notEmpty(Subox) && totatlDetected(UnsolvedBoard) >= 17) {
                    isSolved = true;
                }
                else {

                    isSolved = false;
                }

                cropped2.release();
            }



                Subox = new int[9][9];

              //  tessBaseApi.end();
                slvBtn = false;
                return mRGBa;
            }


        }

         if(isSolved)
             PrintBoard(mRGBa,OnlySolved,TopLeft,BotRight);



            /* Number Drawing



            double xx1 = BotLeft.x + (step1 / 2);
            double yy1 = BotLeft.y - (step2 / 2);



            */

        return mRGBa;
    }



    /*public void PrintBoard(Mat mRGBa,int [][] board, Point TopLeft, Point BotRight, double HeightStep, double WidthStep)
    {
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 8; y++) {
                if (board[y][x] != 0)
                    Imgproc.putText(mRGBa, board[y][x] + "", new Point((TopLeft.x + (HeightStep / 2) + (HeightStep * x)) - 12, (TopLeft.y + (WidthStep / 2) + (WidthStep * y)) + 20),
                            1, 3.0f, new Scalar(0, 255, 0, 0), 4);
            }
        }

    }*/

     public void PrintBoard(Mat mRGBa,int [][] board, Point TopLeft, Point BotRight)
    {

        double HeightStep = (BotRight.x - TopLeft.x) / 9;
        double WidthStep = (BotRight.y - TopLeft.y)/ 9;
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 8; y++) {
                if (board[y][x] != 0)
                    Imgproc.putText(mRGBa, board[y][x] + "", new Point((TopLeft.x + (HeightStep / 2) + (HeightStep * x)) - 12, (TopLeft.y + (WidthStep / 2) + (WidthStep * y)) + 20),
                            1, 3.0f, new Scalar(0, 255, 0, 0), 4);
            }
        }

    }

    private class Solver{

        int[][] puzzle;
        Context context;
        public Solver(int[][] puzzle) {
            this.puzzle = puzzle;
        }

        public int check (int row, int col, int num){

            int rowStart = (row / 3) * 3;
            int colStart = (col / 3) * 3;
            int i;
            for (i = 0; i < 9; i++) {
                if (puzzle[row][i] == num) {
                    return 0;
                }
                if (puzzle[i][col] == num) {
                    return 0;
                }
                if (puzzle[rowStart + (i % 3)][colStart + (i / 3)] == num) {
                    return 0;
                }
            }
            return 1;
        }



        public int solve(int row, int col) {
            if (row < 9 && col < 9) {
                if (puzzle[row][col] != 0) {
                    if ((col + 1) < 9)
                        return solve(row, col + 1);
                    else if ((row + 1) < 9)
                        return solve(row + 1, 0);
                    else
                        return 1;
                } else {
                    for (int i = 0; i < 9; i++) {
                        if (check(row, col, i + 1) == 1) {
                            puzzle[row][col] = i + 1;
                            if (solve(row, col) == 1)
                                return 1;
                            else
                                puzzle[row][col] = 0;
                        }
                    }
                }
                return 0;
            } else return 1;
        }

        public int[][] mainSolver() {
            int[][] result = new int[9][9];

            if (solve(0, 0) == 1) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        result[i][j] = puzzle[i][j];
                    }
                }
                String s="";
                for (int i = 0; i < 9; i++) {
                    s = s + Arrays.toString(puzzle[i]) + " \n";
                }


            }


            return puzzle;
        }


    }

    public boolean notEmpty(int [][]arr)
    {
        for(int i=0;i<=8;i++) {
            for (int j = 0; j <= 8; j++) {
                if (arr[i][j] != 0)
                    return true;
            }
        }

        return false;
    }

    public int totatlDetected(int [][]arr)
    {
        int counter =0;
        for(int i=0;i<=8;i++) {
            for (int j = 0; j <= 8; j++) {
                if (arr[i][j] != 0)
                    counter++;
            }
        }

        return counter;
    }


    public Mat PerspectiveTransformPrint(Point[] srcPoints,double maxHeight,double maxWidth, Mat originalMat)
    {
        MatOfPoint2f src = new MatOfPoint2f(
                srcPoints[0],
                srcPoints[1],
                srcPoints[2],
                srcPoints[3]);

        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0,0),
                new Point(maxWidth,0),
                new Point(0,maxHeight),
                new Point(maxWidth,maxHeight));



        Mat wrappedMat = Imgproc.getPerspectiveTransform(src,dst);
        Mat destMat = new Mat();
        Size size = new Size(maxWidth,maxHeight);

        Imgproc.warpPerspective(originalMat,destMat,wrappedMat,size);

        return destMat;
    }

    public Mat PerspectiveTransform(Point[] srcPoints,double maxHeight,double maxWidth, Mat originalMat)
    {
        MatOfPoint2f src = new MatOfPoint2f(
                srcPoints[0],
                srcPoints[1],
                srcPoints[2],
                srcPoints[3]);

        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0,0),
                new Point(maxWidth,0),
                new Point(0,maxHeight),
                new Point(maxWidth,maxHeight));



        Mat wrappedMat = Imgproc.getPerspectiveTransform(src,dst);
        Mat destMat = new Mat();
        Size size = new Size(maxWidth,maxHeight);

        Imgproc.warpPerspective(originalMat,destMat,wrappedMat,size);

        return destMat;
    }

    public Rect cropBiggestSquare(Mat Pic) {

        Mat mGray = new Mat();
        Imgproc.cvtColor(Pic.clone(), mGray, Imgproc.COLOR_RGB2GRAY);
        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(mGray, blurMat, new Size(5, 5), 0);
        Mat threshMat = new Mat();
        Imgproc.adaptiveThreshold(blurMat, threshMat, 255, 1, 1, 11, 2);


        contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(threshMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        MatOfPoint2f biggest = new MatOfPoint2f();
        double maxArea = 0;
        for (MatOfPoint i : contours) {
            double area = Imgproc.contourArea(i);
            if (area > 100) {
                MatOfPoint2f k = new MatOfPoint2f(i.toArray());
                double peri = Imgproc.arcLength(k, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(k, approx, 0.02 * peri, true);
                if (area > maxArea && approx.total() == 4) {
                    biggest = approx;
                    maxArea = area;
                }
            }
        }

        blurMat.release();
        threshMat.release();
        Point[] points = biggest.toArray();
        Point min = points[0];
        Point max = points[0];
        for(Point i : points)
        {
            if((i.x + i.y) > (max.x + max.y ))
                max = i;

            if(i.x + i.y < (min.x + min.y) )
                min = i;
        }

        Rect r = new Rect(min,max);
        return r;
    }
}







