package com.sample.demoopencv3;

import android.annotation.SuppressLint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;

import com.sample.demoopencv3.ui.VerticalSeekBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 Приложение для демонстрации возможность библиотеки OpenCV
 */

/**
 * Класс главного окна
 */
public class Main extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "LOG_TAG";
    /**
     * Указывает на то что надо скрыть кнопки управления
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     *  {@link #AUTO_HIDE} Задержка перед анимацией исчезновения кнопок
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     Длительность анимации исчезновения кнопок
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();

    /**
     * Используются при переключении между полноэкранным режимом и обычным
     */
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Объект для работы с камерой
     */
    private CameraBridgeViewBase mOpenCvCameraView;


    // Переменные типа матрица, для оработки изображений
    Mat mRgba;
    Mat mGray;
    Mat mGauss;
    Mat mCanny;

    /**
     * Верхний и нижний пороги яркости. используются для алгоритма Canny86
     */
    int mThreshold1=80;
    int mThreshold2=100;


    //Константы типа результата
    final int RES_GRAY      = 1001;
    final int RES_CANNY     = 1002;
    final int RES_CIRCLES   = 1003;

    //Выбраный тип результата
    int mResult = RES_CIRCLES;


    private VerticalSeekBar mVerticalSeekBar1;
    private VerticalSeekBar mVerticalSeekBar2;


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


    /**
     * Обрабатывает нажатие кнопок. Реализовано через OnTouch
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int id = view.getId();
            switch (id){
                case R.id.btnGray : mResult = RES_GRAY; break;
                case R.id.btnCanny : mResult = RES_CANNY; break;
                case R.id.btnCircles : mResult = RES_CIRCLES; break;
                default: break;
            }
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    /**
     * Инициализирует объекты UI и переменные приложения.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVerticalSeekBar1 = (VerticalSeekBar) findViewById(R.id.seekBar1);

        mVerticalSeekBar1.setOnSeekBarChangeListener(
                new VerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mThreshold1 = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mVerticalSeekBar2 = (VerticalSeekBar) findViewById(R.id.seekBar2);

        mVerticalSeekBar2.setOnSeekBarChangeListener(
                new VerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mThreshold2 = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);

        mOpenCvCameraView = (JavaCameraView)
                findViewById(R.id.show_camera_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);


        // обработка нажатия по картинке
        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Ограничение размера фрейма для увеличения скорости обработки
        mOpenCvCameraView.setMaxFrameSize(640, 480);

        // присвоения кнопкам управления слушателя для обработки нажатия
        findViewById(R.id.btnGray).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.btnCanny).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.btnCircles).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //Сокрытие кнопок с задержкой 100 мс.
        delayedHide(100);
    }

    //Реализует переключатель видимости кнопок
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    // Скрывает кнопки, переводит в полноэкранный режим
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    // Отображает кнопки и строку заголовка
    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     *
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    //Остановка отображения при выходе из приложения, но не закрытии
    @Override
    public void onPause() {
        super.onPause();

        if (mOpenCvCameraView != null)

            mOpenCvCameraView.disableView();
    }

    //Возобновление показа
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            //Запуск модуля OpenCV d ljgjkybntkmyjv gjnjrt
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //Выгрузка обекта mOpenCvCameraView из памяти при закрытии
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    //Инициализирует матрицы для дальнейшей работы. Задаются размеры и типы точек
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
        mCanny = new Mat(height, width, CvType.CV_8UC4);
        mGauss = new Mat(height, width, CvType.CV_8UC4);
    }

    //Выгрузка матриц при остановке работы с камерой
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mCanny.release();
    }

    //Сообщает о получении картинки с камеры
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();                                  //Запись цветной матрицы

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);    //Сохдание ЧБ матрицы

        if (mResult==RES_GRAY) return mGray;                        //Возрат ЧБ матрицы

        Size s = new Size(5,5);                                     //Шаблон для размытия

        Imgproc.GaussianBlur(mGray, mGray, s, 2, 2);                //Размытие картинки по Гауссу

        Core.inRange(mGray                                          //Выделение только
                , new Scalar(mThreshold1)                           // средних полутонов
                , new Scalar(mThreshold2), mGray);

        Imgproc.Canny(mGray , mCanny, mThreshold1, mThreshold2);    //Создание матрицы контуров

        if (mResult==RES_CANNY) return mCanny;                      //Возврат матрицы Canny


        Mat circles = new Mat();                                    //Поиск возможных кругов
        Imgproc.HoughCircles(mGray, circles,
                        Imgproc.CV_HOUGH_GRADIENT, 1, 75, 50, 13, 10, 100);
        //if(true) return mCanny;
//
        if (circles.cols() > 0)                                     //Нанесение найденых кругов

            for (int x = 0; x < circles.cols(); x++){

                double vCircle[] = circles.get(0,x);

                if (vCircle == null) break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));

                int radius = (int)Math.round(vCircle[2]);

                // Отрисовка кругов
                Imgproc.circle(mRgba, pt, radius, new Scalar(0,255,0), 2);

                Imgproc.circle(mRgba, pt, 3, new Scalar(0,0,255), 2);
            }

        return mRgba;                                               //Вывод результата
    }
}
