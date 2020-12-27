#include <M5Core2.h>
#include <driver/i2s.h>
#include <WebServer.h>
#include "AudioFileSourceSD.h"
#include "AudioFileSourceID3.h"
#include "AudioGeneratorMP3.h"
#include "AudioOutputI2S.h"
AudioGeneratorMP3 *mp3;
AudioFileSourceSD *file;
AudioOutputI2S *out;
AudioFileSourceID3 *id3;
#define OUTPUT_GAIN 10

typedef enum {CARD_BASE, CARD_CORE, CARD_BALA, CARD_NUM}CARDNAME;

//各カードのIDを設定する。
const char* idList[] = {
  "********", //BASE
  "********", //CORE
  "********"  //BALA
};

WiFiClient client;
WebServer webServer(80);

const char* ssid = "[YOUR_SSID]";
const char* password = "[YOUR_PASSWORD]";

int posIndex = 0;
static TFT_eSprite fb = TFT_eSprite(&M5.Lcd);

void setup() {
  M5.begin(true, true, true, true);
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setTextColor(WHITE);
  M5.Lcd.setTextSize(2);
  
  M5.Axp.SetSpkEnable(true);
  startWebServer();

  fb.createSprite(100, 240);
  fb.fillSprite(TFT_BLACK);
}

void loop() {
  webServer.handleClient();
  delay(100);
}


void startWebServer() {
  WiFi.disconnect(true);
  delay(1000);
  WiFi.begin(ssid, password);  //  Wi-Fi APに接続
  int lpcnt = 0;
  int lpcnt2 = 0;
  while (WiFi.status() != WL_CONNECTED) {  //  Wi-Fi AP接続待ち
      delay(500);                            //   0.5秒毎にチェック
      lpcnt += 1 ;                           //
      if (lpcnt > 6) {                       // 6回目(3秒) で切断/再接続
        WiFi.disconnect(true,true) ;         //
        WiFi.begin(ssid, password);   //
        lpcnt = 0 ;                          //
        lpcnt2 += 1 ;                        // 再接続の回数をカウント
      }                                      //
      if (lpcnt2 > 3) {                      // 3回 接続できなければ、
        ESP.restart() ;                      // ソフトウェアリセット
      }                                      //
      Serial.print(".");                     //
  }    
  delay(1000);

  webServer.on("/m5card", m5card);
  webServer.begin();
  M5.Lcd.println("http://" + WiFi.localIP().toString());
}

void m5card() {
  String id = webServer.arg("id");
  webServer.send(200, "text/plain", "OK!");
  
  Serial.println(id);
  int selectedCard = -1;
  for(int index = 0;index < CARD_NUM;index++){
    if(id.equals(String(idList[index]))==true){
      selectedCard = index;
      break;
    }
  }
  if(selectedCard >= 0){
    dispCard(selectedCard);
  }else{
    playMp3("/rfidCard/release.mp3");
    M5.Lcd.fillScreen(BLACK);
  }
}

void dispCard(int index){
  Serial.println(index);
  
  switch(index){
    case CARD_BASE:
    fb.drawJpgFile(SD, "/rfidCard/base.jpg", 0,0);
    cardSet();
    playMp3("/rfidCard/base.mp3");
    break;
    case CARD_CORE:
    fb.drawJpgFile(SD, "/rfidCard/core.jpg", 0,0);
    cardSet();
    playMp3("/rfidCard/core.mp3");
    break;
    case CARD_BALA:
    fb.drawJpgFile(SD, "/rfidCard/bala.jpg", 0,0);
    cardSet();
    playMp3("/rfidCard/bala.mp3");
    break;
  }
  
  posIndex++;
  if(posIndex >= 3){
    posIndex = 0;
    delay(500);
    playMp3("/rfidCard/allSet.mp3");
  }
}

void cardSet(){
  for(int yOffset = 0;yOffset <= 24;yOffset++){
    fb.pushSprite(posIndex * 100 + 10, yOffset * 10 - 240);  
  }
  playMp3("/rfidCard/set.mp3");
}

void playMp3(char *fileName){
  file = new AudioFileSourceSD(fileName);
  id3 = new AudioFileSourceID3(file);
  out = new AudioOutputI2S(0, 0); // Output to ExternalDAC
  out->SetPinout(12, 0, 2);
  out->SetOutputModeMono(true);
  out->SetGain((float)OUTPUT_GAIN/100.0);
  mp3 = new AudioGeneratorMP3();
  mp3->begin(id3, out);
  while(mp3->isRunning()){
    if (!mp3->loop()) mp3->stop();
  }
  delete file;
  delete id3;
  delete out;  
  delete mp3;
  
}
