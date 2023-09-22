package com.hifi.redeal.incoming

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class PhoneCallReceiver: BroadcastReceiver() {
    companion object{
        // 리시버가 중복 호출 되는 문제를 해결 하기 위한 변수 선언.
        var lastState = true
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            // 사용자 단말기에 전화를 건 전화번호
            val incomingNumber = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) { // 현재 단말기의 통화 상태를 가져온다.
                TelephonyManager.EXTRA_STATE_RINGING -> { // 전화 벨이 울리는 경우 -> 전화 수신 상태
                    // 여기에서 전화번호를 사용하거나 저장할 수 있습니다.

                    if(incomingNumber != null && lastState){
                        lastState = false
                        Log.d("tttt","통화 수신 로그 / 실행 시간 : ${System.currentTimeMillis()}")
                        val serviceIntent = Intent(context, CallNumberCheckService::class.java)
                        serviceIntent.putExtra("incomingNumber", "$incomingNumber")

                        // 버전 별로 분기 하여 진행한다. -> 안드로이드 12버전 이상 부터는 백그라운드에서 포그라운드 서비스 실행이 특정 상황에서는 허용된다.
                        // 12버전 이상에서 포어그라운드를 실행할 경우 앱의 액티비티가 테스크에 없다면 실행이 되지 않는다.

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null){
                            CallNumberCheckWorker.incommingNumber = incomingNumber
                            val request = OneTimeWorkRequestBuilder<CallNumberCheckWorker>()
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                            WorkManager.getInstance(context).enqueue(request)
                            Log.d("tttt","통화 수신 워커 실행 / 실행 시간 : ${System.currentTimeMillis()}")
                        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
                            ContextCompat.startForegroundService(context, serviceIntent)
                            Log.d("tttt","통화 수신 서비스 실행 / 실행 시간 : ${System.currentTimeMillis()}")
                        } else {
                            context?.startService(serviceIntent)
                            Log.d("tttt","통화 수신 서비스 실행 낮은 버전 / 실행 시간 : ${System.currentTimeMillis()}")
                        }

                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> { // 통화 중 상태

                }
                TelephonyManager.EXTRA_STATE_IDLE -> { // 통화가 종료 된 상태
                    // 통화 종료 처리
                    if(context != null && !lastState && incomingNumber != null) {
                        lastState = true
                        // 서비스를 종료하는 Intent 생성
                        val stopServiceIntent = Intent(context, CallNumberCheckService::class.java)
                        // 서비스 종료
                        context.stopService(stopServiceIntent)
                        Log.d("tttt","통화 수신 서비스 종료 / 종료 시간 : ${System.currentTimeMillis()}")
                    }
                }
            }
        }
    }


}