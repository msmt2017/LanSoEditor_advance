package com.example.advanceDemo.camera;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.LanSongGrindFilter;


import com.example.advanceDemo.GPUImageFilterTools;
import com.example.advanceDemo.VideoPlayerActivity;
import com.example.advanceDemo.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import com.example.advanceDemo.view.CameraProgressBar;
import com.example.advanceDemo.view.FaceView;
import com.example.advanceDemo.view.FocusImageView;
import com.lansoeditor.demo.R;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.BitmapLoader;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.DrawPad;
import com.lansosdk.box.Layer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.MVLayerENDMode;
import com.lansosdk.box.AudioPad;
import com.lansosdk.box.SampleSave;
import com.lansosdk.box.SubLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.ViewLayerRelativeLayout;
import com.lansosdk.box.onDrawPadErrorListener;
import com.lansosdk.box.onDrawPadOutFrameListener;
import com.lansosdk.box.onDrawPadPreviewProgressListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSnapShotListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;
import com.lansosdk.videoeditor.AVDecoder;
import com.lansosdk.videoeditor.CopyDefaultVideoAsyncTask;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.DrawPadCameraView;
import com.lansosdk.videoeditor.DrawPadCameraView.doFousEventListener;
import com.lansosdk.videoeditor.DrawPadCameraView.onViewAvailable;
import com.lansosdk.videoeditor.DrawPadView;
import com.lansosdk.videoeditor.FileWriteUtils;
import com.lansosdk.videoeditor.LanSongUtil;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.Camera.Face;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class CameraLayerFullPortWithMp3Activity extends Activity implements OnClickListener{
   
	
	private static final int RECORD_CAMERA_MIN=2*1000*1000; //定义最小2秒
	
	private static final String TAG = "CameraFullRecordActivity";

    private DrawPadCameraView mDrawPadCamera;
    
    private CameraLayer  mCamLayer=null;
    
    private String dstPath=null;  //用于录制完成后的目标视频路径.
    
	private FocusImageView focusView;
	
	private PowerManager.WakeLock mWakeLock;
    private TextView  tvTime;
    private Context mContext=null;
    

    private ImageView btnOk;
    private Button btnRecord;
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        //全屏模式下, 隐藏底部的虚拟按键.
        LanSongUtil.hideBottomUIMenu(this);
        mContext=getApplicationContext();
        
        if(LanSongUtil.checkRecordPermission(getBaseContext())==false){
       	   Toast.makeText(getApplicationContext(), "当前无权限,请打开权限后,重试!!!", Toast.LENGTH_LONG).show();
       	   finish();
         }
        
        setContentView(R.layout.camera_fullport_mp3_layout);
        mDrawPadCamera = (DrawPadCameraView) findViewById(R.id.id_fullrecord_padview);
        
        initView();
        btnRecord.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mDrawPadCamera!=null){
            		/**
            		 * 这里只是暂停和恢复录制, 可以录制多段,但不可以删除录制好的每一段,
            		 * 如果你要分段录制,并支持回删,则可以采用SegmentStart和SegmentStop;
            		 */
            		if(mDrawPadCamera.isRecording()){
            			mDrawPadCamera.pauseRecord();  
            		}else{
            			/**
            			 * 把用户选择好的歌曲,设置到drawpad容器中, 开始录制.
            			 */
            			String music=CopyFileFromAssets.copyAssets(getApplicationContext(), "wenbie_5m_2s.mp3");
            			mDrawPadCamera.setRecordExtraMp3(music, true);
            			mDrawPadCamera.startRecord();
            		}
            	}
			}
		});
        dstPath=SDKFileUtils.newMp4PathInBox();
		initDrawPad();  //开始录制.
    }
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
        LanSongUtil.hideBottomUIMenu(this);
    	super.onResume();
    	if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
			mWakeLock.acquire();
		}
		startDrawPad();
    }
                                    
    /**
     * Step1: 开始运行 DrawPad 画板
     */
    private void initDrawPad()
    {
    	 int padWidth=544;  
    	 int padHeight=960;
    	 int bitrate=3000*1024;  //码率
    	/**
    	 * 设置录制时的一些参数. 
    	 */
    	mDrawPadCamera.setRealEncodeEnable(padWidth,padHeight,bitrate,(int)25,dstPath);
    	/**
    	 * 录制的同时,录制外面的声音.
    	 */
  	    mDrawPadCamera.setRecordMic(true);
    	/**
    	 * 设置录制处理进度监听.
    	 */
    	mDrawPadCamera.setOnDrawPadProgressListener(drawPadProgressListener);
    	
    	/**
    	 * 相机前后置.是否设置滤镜.
    	 */
    	mDrawPadCamera.setCameraParam(false, null,true);
    	
    	/**
    	 * 当手动聚焦的时候, 返回聚焦点的位置,让focusView去显示一个聚焦的动画.
    	 */
    	mDrawPadCamera.setCameraFocusListener(new doFousEventListener() {
			
			@Override
			public void onFocus(int x, int y) {
				// TODO Auto-generated method stub
				focusView.startFocus(x, y);
			}
		});
    	/**
    	 * 
    	 * UI界面有效后, 开始开启DrawPad线程, 来预览画面.
    	 */
    	mDrawPadCamera.setOnViewAvailable(new onViewAvailable() {
			
			@Override
			public void viewAvailable(DrawPadCameraView v) {
				// TODO Auto-generated method stub
					startDrawPad();
			}
		});
    	mDrawPadCamera.setOnDrawPadErrorListener(new onDrawPadErrorListener() {
			
			@Override
			public void onError(DrawPad d, int what) {
				// TODO Auto-generated method stub
				Log.e(TAG,"DrawPad画板线程运行出错!!!"+what);
			}
		});
    }
    /**
     * Step2: 开始运行 Drawpad线程.
     */
      private void startDrawPad()
      {
    	  if(mDrawPadCamera.setupDrawpad())
    	  {
    		  mCamLayer=mDrawPadCamera.getCameraLayer();
    		  addBitmapLayer();
    		  mDrawPadCamera.startPreview();
    	  }else{
    		  Log.i(TAG,"建立drawpad线程失败.");
    	  }
      }
      /**
       * Step3: 停止画板, 停止后,为新的视频文件增加上音频部分.
       */
      private void stopDrawPad()
      {
	      	if(mDrawPadCamera!=null && mDrawPadCamera.isRunning())
	      	{
	  				mDrawPadCamera.stopDrawPad();
	  				mCamLayer=null;
	  		}
      }
    private onDrawPadProgressListener drawPadProgressListener=new onDrawPadProgressListener() {
		
		@Override
		public void onProgress(DrawPad v, long currentTimeUs) {
			// TODO Auto-generated method stub
			
			if(currentTimeUs>=RECORD_CAMERA_MIN && btnOk!=null){
				btnOk.setVisibility(View.VISIBLE);
			}
			if(tvTime!=null){
				float timeF=((float)currentTimeUs/1000000);
				float b   =  (float)(Math.round(timeF*10))/10;  //保留一位小数.
				
				if(b>=0)
					tvTime.setText(String.valueOf(b));
			}
		}
	};
    /**
     * 选择滤镜效果, 
     */
    private void selectFilter()
    {
    	if(mDrawPadCamera!=null && mDrawPadCamera.isRunning()){
    		GPUImageFilterTools.showDialog(this, new OnGpuImageFilterChosenListener() {

                @Override
                public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                	if(mCamLayer!=null)
                	{
                		mCamLayer.switchFilterTo(filter);	
                	}
                }
            });
    	}
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	if(mDrawPadCamera!=null){
    		mDrawPadCamera.stopDrawPad();
    	}
    	if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
    	}
    }
   @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
			super.onDestroy();
			stopDrawPad();
			
		    if(SDKFileUtils.fileExist(dstPath)){
		    	SDKFileUtils.deleteFile(dstPath);
		    	dstPath=null;
		    }
	}
   private BitmapLayer  bmpLayer;
   private void addBitmapLayer()
   {
	   	if(mDrawPadCamera!=null && mDrawPadCamera.isRunning())
		{
			String bitmapPath=CopyFileFromAssets.copyAssets(getApplicationContext(), "small.png");
			bmpLayer=mDrawPadCamera.addBitmapLayer(BitmapFactory.decodeFile(bitmapPath));
			
			//把位置放到中间的右侧, 因为获取的高度是中心点的高度.
			bmpLayer.setPosition(bmpLayer.getPadWidth()-bmpLayer.getLayerWidth()/2,bmpLayer.getPositionY());
		}
   }
   //-------------------------------------------一下是UI界面和控制部分.---------------------------------------------------
   private void initView()
   {
	   		findViewById(R.id.id_fullrecord_cancel).setOnClickListener(this);

	        tvTime=(TextView)findViewById(R.id.id_fullscreen_timetv);
	        
	   		btnOk=(ImageView)findViewById(R.id.id_fullrecord_ok);
	   		btnOk.setOnClickListener(this);
	   		
	   
	   		focusView=(FocusImageView)findViewById(R.id.id_fullrecord_focusview);
	   
	   		findViewById(R.id.id_fullrecord_flashlight).setOnClickListener(this);
			findViewById(R.id.id_fullrecord_frontcamera).setOnClickListener(this);
			findViewById(R.id.id_fullrecord_filter).setOnClickListener(this);
			btnRecord=(Button)findViewById(R.id.id_fullrecord_button);
   }
   private void playVideo()
   {
	   if(SDKFileUtils.fileExist(dstPath)){
			 	Intent intent=new Intent(this,VideoPlayerActivity.class);
   	    	intent.putExtra("videopath", dstPath);
   	    	startActivity(intent);
		 }else{
			 Toast.makeText(this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		 }
   }
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
			case R.id.id_fullrecord_cancel:
				
				this.finish();
				break;
			case R.id.id_fullrecord_ok:
				stopDrawPad();
				playVideo();
				break;
			case R.id.id_fullrecord_frontcamera:
				if(mCamLayer!=null){
					if(mDrawPadCamera.isRunning() && CameraLayer.isSupportFrontCamera())  
					{
						//先把DrawPad暂停运行.
						mDrawPadCamera.pausePreview();
						mCamLayer.changeCamera();	
						mDrawPadCamera.resumePreview(); //再次开启.
					}
				}
				break;
			case R.id.id_fullrecord_flashlight:
				if(mCamLayer!=null){
					mCamLayer.changeFlash();
				}
				break;
			case R.id.id_fullrecord_filter:
					selectFilter();
				break;
		default:
			break;
		}
	}
}