/* RoverBluetooth by marcocipriani01
   CC BY-SA
   https://marcocipriani01.github.io/projects/RoverBluetooth
*/

#include <Servo.h>

Servo servo;

const byte Mpin = 5;          //L6203 Motor pin
const byte BMpin = 11;          //L6203 Motor pin
const byte enablePin = 8;      //L6203 EN pin
const byte trig = 7;           //Trigger pin of the ultrasonic sensor
const byte echo = 6;           //Echo pin of the ultrasonic sensor
const byte buzzer = 12;        //Buzzer pin
const byte LED = 2;            //Light

boolean state = false;            //State
unsigned long val = 0;            //Distance
unsigned long lastime = 0;        //Counter for the update rate of the proximity sensor

void setup() {
  servo.attach(3);
  pinMode(Mpin, OUTPUT);
  pinMode(BMpin, OUTPUT);
  pinMode(enablePin, OUTPUT);
  pinMode(trig, OUTPUT);
  pinMode(echo, INPUT);
  pinMode(LED, OUTPUT);

  Serial.begin(115200);           //Start the BT connection

  tone(12, 2000);
  goTo(0, 0);
  servo.write(90);
  delay(500);
  noTone(12);
}

void loop() {
  if (Serial.available()) {
    val = Serial.parseInt();
    if (val == 21) {                                //Off
      goTo(0, 0);
      state = false;
    } else if (val < 20) {
      servo.write(constrain(map(val, 0, 20, 140, 40), 40, 140));
    } else if (val == 22) {                         //LED On
      digitalWrite(LED, HIGH);
    } else if (val == 23) {                         //LED Off
      digitalWrite(LED, LOW);
    } else if ((val >= 1500) && (val <= 1755)) {    //Backwards
      val = val - 1500;
      goTo(2, val);
      state = true;
    } else if ((val >= 1000) && (val <= 1255)) {    //On
      val = val - 1000;
      goTo(1, val);
      state = true;
    }
  }

  unsigned long now = millis();
  if (now - lastime >= 300) {                               //Check the distance every 300 ms
    lastime = now;
    if (getDistance() < 40) {                               //If near a wall...
      delay(30);

      unsigned long distance = getDistance();
      if (distance < 40) {                                  //Check another time the distance (to be sure...)
        if (state == true) {                                //If turned on, the car go backwards
          goTo(2, 210);
        }

        byte pulses = map(distance, 2, 40, 20, 1);
        unsigned int msPerPulse = 1000 / (pulses * 2);
        for (byte index = 1; index <= pulses; index++) {    //Play a frequency based on the distance
          tone(12, 2000);
          delay(msPerPulse);
          noTone(12);
          delay(msPerPulse);
        }

        if (state == true) {
          goTo(0, 0);
          delay(100);
        }
      }
    }
  }
}

long getDistance() {
  long distance;

  digitalWrite(trig, LOW);
  delayMicroseconds(2);
  digitalWrite(trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(trig, LOW);

  distance = pulseIn(echo, HIGH);
  distance = distance / 58;

  Serial.println(distance);
  return distance;
}

void goTo(byte dir, byte Speed) {   //Go backwards and forwards
  if (dir == 1) {
    analogWrite(BMpin, 0);
    digitalWrite(enablePin, HIGH);
    analogWrite(Mpin, Speed);
  } else if (dir == 2) {
    analogWrite(Mpin, 0);
    digitalWrite(enablePin, HIGH);
    analogWrite(BMpin, Speed);
  } else if (dir == 0) {
    digitalWrite(enablePin, LOW);
    analogWrite(Mpin, 0);
    analogWrite(BMpin, 0);
  }
}
