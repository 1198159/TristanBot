package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

public class TTRobot {
  enum LinearSlidePosition {
    In,
    middleIn,
    middleOut,
    Out
  }

  // The power applied to the wheels for robot rotation
  private static final double TURNSPEEDFACTOR = 0.5;
  // the grab rotation position for snapping to horizontal or vertical
  private static final double GRABBERPOSITIONCUTOFF = 0.25;
  // the grab rotation 'horizontal' position
  private static final double HORIZONTALGRABBERPOSITION = 0.0;
  // the grab rotation 'vertical' position
  private static final double VERTICALGRABBERPOSITION = 0.5;
  // the power of the linear slide
  private static final double LINEARSLIDEPOWER = -0.5;

  // This is the middle 'dead zone' of the analog sticks
  public static final double STICKDEADZONE = 0.25;

  public static final double TRIGGERTHRESHOLD = 0.25;

  private boolean isGrabberOpened = true;
  private LinearSlidePosition position = LinearSlidePosition.In;

  private DigitalChannel lslideSwitch = null;
  private DigitalChannel liftSwitch = null;
  private CRServo slide = null;
  private DcMotor flMotor = null;
  private DcMotor frMotor = null;
  private DcMotor rlMotor = null;
  private DcMotor rrMotor = null;
  private DcMotor lLiftMotor = null;
  private DcMotor rLiftMotor = null;
  private Servo turn = null;
  private Servo claw = null;
  private TouchSensor extended = null;
  private TouchSensor retracted = null;
  private Servo basePlateGrabber = null;
  private TouchSensor touch = null;
  private CRServo cap = null;

  private Telemetry telemetry = null;
  // Stuff for the on-board "inertial measurement unit" (aka gyro)
  // The IMU sensor object
  private BNO055IMU imu;
  // State used for updating telemetry
  private Orientation angles;
  private Acceleration gravity;

  private final void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public TTRobot() {
  }

  public void init(HardwareMap hardwareMap, Telemetry tel) {
    telemetry = tel;
    // Get handles to all the hardware
    slide = hardwareMap.get(CRServo.class, "servo");
    turn = hardwareMap.get(Servo.class, "grabTurn");
    claw = hardwareMap.get(Servo.class, "claw");
    basePlateGrabber = hardwareMap.get(Servo.class, "bpGrabber");
    cap = hardwareMap.get(CRServo.class, "cap");
    // extended = hardwareMap.get(TouchSensor.class, "extLimitSwitch");
    // retracted = hardwareMap.get(TouchSensor.class, "retLimitSwitch");
    lslideSwitch = hardwareMap.get(DigitalChannel.class, "slideLimit");
    liftSwitch = hardwareMap.get(DigitalChannel.class, "liftLimit");

    flMotor = hardwareMap.get(DcMotor.class, "motorFrontLeft");
    frMotor = hardwareMap.get(DcMotor.class, "motorFrontRight");
    rlMotor = hardwareMap.get(DcMotor.class, "motorRearLeft");
    rrMotor = hardwareMap.get(DcMotor.class, "motorRearRight");

    lLiftMotor = hardwareMap.get(DcMotor.class, "motorLiftLeft");
    rLiftMotor = hardwareMap.get(DcMotor.class, "motorLiftRight");

    //    touch = hardwareMap.get(TouchSensor.class, "touch");

    // Setup the IMU
    // Set up the parameters with which we will use our IMU. Note that integration
    // algorithm here just reports accelerations to the logcat log; it doesn't actually
    // provide positional information.
    BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
    parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
    parameters.calibrationDataFile =
      "AdafruitIMUCalibration.json"; // see the calibration sample opmode
    imu = hardwareMap.get(BNO055IMU.class, "imu1");
    imu.initialize(parameters);
  }

  public void calibrate() {
    // make lift motors work together: they're facing opposite directions
    lLiftMotor.setDirection(DcMotorSimple.Direction.FORWARD);
    rLiftMotor.setDirection(DcMotorSimple.Direction.REVERSE);
    // Set the digital channel mode to input
    // Output mode can be used to blink LED's
    lslideSwitch.setMode(DigitalChannel.Mode.INPUT);
    liftSwitch.setMode(DigitalChannel.Mode.INPUT);

    // TODO: Add initialization for the slide and lift?

    // Shamelessly copied from example code...
    while (imu.getCalibrationStatus().calibrationStatus != 0
      || imu.getSystemStatus() != BNO055IMU.SystemStatus.RUNNING_FUSION) {
      sleep(10);
    }
    // Start the logging of measured acceleration
    imu.startAccelerationIntegration(new Position(), new Velocity(), 1000);
  }

  // Linear slide stuff:
  public boolean isLinearSlideFullyExtended() {
    return extended.isPressed();
  }

  public boolean isLinearSlideFullyRetracted() {
    return retracted.isPressed();
  }

  private void moveSlideNext(LinearSlideOperation op) {
    double power = (op == LinearSlideOperation.Extend) ? LINEARSLIDEPOWER : -LINEARSLIDEPOWER;
    while (!lslideSwitch.getState()) {
      sleep(10);
      slide.setPower(power);
    }
    while (lslideSwitch.getState()) {
      sleep(10);
      slide.setPower(power);
    }
    slide.setPower(0);
  }

  public void lslide(LinearSlideOperation inOrOut) {
    if (position == LinearSlidePosition.In) {
      if (inOrOut == LinearSlideOperation.Extend) {
        moveSlideNext(LinearSlideOperation.Extend);
        position = LinearSlidePosition.middleIn;
      }
    } else if (position == LinearSlidePosition.middleIn) {
      if (inOrOut == LinearSlideOperation.Extend) {
        moveSlideNext(LinearSlideOperation.Extend);
        position = LinearSlidePosition.middleOut;
      } else {
        moveSlideNext(LinearSlideOperation.Retract);
        position = LinearSlidePosition.In;
      }
    } else if (position == LinearSlidePosition.middleOut) {
      if (inOrOut == LinearSlideOperation.Extend) {
        moveSlideNext(LinearSlideOperation.Extend);
        position = LinearSlidePosition.Out;
      } else {
        moveSlideNext(LinearSlideOperation.Retract);
        position = LinearSlidePosition.middleIn;
      }
    } else {
      if (inOrOut == LinearSlideOperation.Retract) {
        moveSlideNext(LinearSlideOperation.Retract);
        position = LinearSlidePosition.middleOut;
      }
    }
  }

  // Grabber stuff:
  public void claw(double val) {
    claw.setPosition(val);
  }

  public void turnn(double val) {
    turn.setPosition(val);
  }

  public void setClawPosition(ClawPosition position) {
    switch (position) {
      case Open:
        claw.setPosition(0.4); // Open
        break;
      case Close:
        claw.setPosition(0.6); // Closed
        break;
    }
  }

  public void setGrabberPosition(GrabberPosition position) {
  }

  public void simpleSlide(double speed) {
    slide.setPower(-speed);
  }
/*public void turnn(double speed) {
    turn.setPower(speed);
  }*/

  /*public void snapGrabberPosition(GrabberPosition position) {
    switch (position) {
      case Horizontal:
        turn.setPosition(HORIZONTALGRABBERPOSITION);
        break;
      case Vertical:
        turn.setPosition(VERTICALGRABBERPOSITION);
        break;
    }
  }

  public void turnGrabber(GrabberPosition position) {
    switch (position) {
      case Horizontal:
        turn.setPosition(turn.getPosition() - 0.1);
        break;
      case Vertical:
        turn.setPosition(turn.getPosition() + 0.1);
        break;
    }
  }*/

  // Lift stuff:
  enum LiftState {
    Above,
    At,
    Below
  }

  LiftState liftPosition = LiftState.Below;

  // Override, just in case we're stuck in "only go up" mode
  public void overrideLiftLimit() {
    liftPosition = LiftState.Above;
  }

  private void setLiftPower(double val) {
    lLiftMotor.setPower(val);
    rLiftMotor.setPower(val);
  }

  // Positive is down, Negative is up!
  public void setLift(double speed) {
    // We have to support the limit switch having turned off
    // and then back up
    boolean isLimitSet = !liftSwitch.getState();
    switch (liftPosition) {
      case Above:
        if (isLimitSet)
          liftPosition = LiftState.At;
        setLiftPower(speed);
        break;
      case Below:
        if (isLimitSet)
          liftPosition = LiftState.At;
        setLiftPower(-Math.abs(speed));
        break;
      case At:
        if (!isLimitSet)
          liftPosition = (speed < 0) ? LiftState.Below : LiftState.Above;
        setLiftPower(-Math.abs(speed));
        break;
    }
  }


  private boolean hook = false;

  public void bpGrabber() {

    //TODO
  }

  public void capstone() {
    //TODO
  }

  public boolean isLiftAtUpperLimit() {
    // TODO: Read the upper limit switch
    return false;
  }

  public boolean isLiftAtLowerLimit() {
    // TODO: Read the lower limit switch
    return false;
  }

  // Drive train:
  // These should just be used by the drive train
  public void motorFrontLeft(double power) {
    flMotor.setPower(Range.clip(power, -1, 1));
  }

  public void motorFrontRight(double power) {
    frMotor.setPower(Range.clip(power, -1, 1));
  }

  public void motorRearLeft(double power) {
    rlMotor.setPower(Range.clip(power, -1, 1));
  }

  public void motorRearRight(double power) {
    rrMotor.setPower(Range.clip(power, -1, 1));
  }

  public double gyroHeading() {
    Orientation angles1 =
      imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
    return -AngleUnit.DEGREES.fromUnit(angles1.angleUnit, angles1.firstAngle + 180);
  }

  /*
  fl  +-------+  fr
     /         \
    /           \
   +             +
   |    motor    |
   |   position  |
   +             +
    \           /
     \         /
  rl  +-------+  rr
  */

  // leave gyroAngle at zero to set relative angle
  public void joystickDrive(Direction j1, Direction j2, double gyroAngle) {
    double leftStickY = 0;
    double leftStickX = 0;
    double rightStickX = 0;

    double robotHeadingRad = 0.0;
    double powerCompY = 0.0;
    double powerCompX = 0.0;

    double powerFrontLeft = 0.0;
    double powerFrontRight = 0.0;
    double powerRearLeft = 0.0;
    double powerRearRight = 0.0;

    if (j1.Y != 0) {
      leftStickY = -stepInput(j1.Y);
    } else {
      leftStickY = 0.0;
    }
    if (j1.X != 0) {
      leftStickX = stepInput(j1.X);
    } else {
      leftStickX = 0.0;
    }
    rightStickX = -stepInputRotate(j2.X);

    if (Math.abs(leftStickY) > STICKDEADZONE
      || Math.abs(leftStickX) > STICKDEADZONE
      || Math.abs(rightStickX) > STICKDEADZONE) {
      //                robotHeadingRad = Math.toRadians(((360 - robot.gyro.getHeading()) % 360));
      robotHeadingRad = Math.toRadians(gyroAngle);
      powerCompY =
        (Math.cos(robotHeadingRad) * leftStickY) + (Math.sin(robotHeadingRad) * leftStickX);
      powerCompX =
        -(Math.sin(robotHeadingRad) * leftStickY) + (Math.cos(robotHeadingRad) * leftStickX);

      powerFrontLeft = powerCompY + powerCompX + rightStickX;
      powerFrontRight = -powerCompY + powerCompX + rightStickX;
      powerRearLeft = powerCompY - powerCompX + rightStickX;
      powerRearRight = -powerCompY - powerCompX + rightStickX;
    } else {
      powerFrontLeft = 0.0;
      powerFrontRight = 0.0;
      powerRearLeft = 0.0;
      powerRearRight = 0.0;
    }

    motorFrontLeft(powerFrontLeft);
    motorFrontRight(powerFrontRight);
    motorRearLeft(powerRearLeft);
    motorRearRight(powerRearRight);
  }

  public void drive(double joystickAngle, double gyroAngle, double power, double turn) {
    double tturn = turn * TURNSPEEDFACTOR;
    double angle = joystickAngle + gyroAngle;
    double flPower = power * Math.cos(-Math.PI * angle - Math.PI / 4) + tturn;
    double frPower = -power * Math.cos(-Math.PI * angle + Math.PI / 4) + tturn;
    double rrPower = -power * Math.cos(-Math.PI * angle - Math.PI / 4) + tturn;
    double rlPower = power * Math.cos(-Math.PI * angle + Math.PI / 4) + tturn;
    telemetry.addData(
      "drive", "fl %3.2f fr %3.2f rr %3.2f rl %3.2f", flPower, frPower, rrPower, rlPower);
    this.motorFrontLeft(flPower);
    this.motorFrontRight(frPower);
    this.motorRearLeft(rlPower);
    this.motorRearRight(rrPower);
  }

  // set nearestSnap to true to snap to nearest 90 dgree angle, or set nearestSnap to false and
  // input angle to snap to.
  public double snapToAngle(double gyroAngle) {
    double test = 0.0;
    if (gyroAngle > 50 && gyroAngle < 130) {
      test = 90 - gyroAngle;

    } else if (gyroAngle > 140 && gyroAngle < 220) {
      test = 180 - gyroAngle;

    } else if (gyroAngle > 230 && gyroAngle < 310) {
      test = 270 - gyroAngle;

    } else if ((gyroAngle >= 0 && gyroAngle < 40) || (gyroAngle > 320 && gyroAngle <= 360)) {
      test = 0 - gyroAngle;
    }
    return test;
  }

  // Snap the robot to the closest 90 degree angle
  public void snap() {
    double curr = gyroHeading();
    double newangle = snapToAngle(curr);
    snap(newangle);
  }

  // Turn the robot to a specific angle
  public void snap(double angle) {
    drive(0.0, 0.0, 0.0, angle);
  }

  public void timeDrive(double speed, double time, double angle) {
    ElapsedTime driveTime = new ElapsedTime();
    double robotHeadingRad = 0.0;
    double angleRad = Math.toRadians(angle);
    double powerCompY = 0.0;
    double powerCompX = 0.0;

    double frontLeftSpeed;
    double frontRightSpeed;
    double rearLeftSpeed;
    double rearRightSpeed;

    driveTime.reset();

    speed = Range.clip(speed, 0.0, 1.0);
    //            robotHeadingRad = Math.toRadians(360 - robot.gyro.getHeading());
    robotHeadingRad = Math.toRadians(this.gyroHeading());
    powerCompY =
      (Math.cos(robotHeadingRad) * (Math.cos(angleRad) * speed))
        + (Math.sin(robotHeadingRad) * (Math.sin(angleRad) * speed));
    powerCompX =
      -(Math.sin(robotHeadingRad) * (Math.cos(angleRad) * speed))
        + (Math.cos(robotHeadingRad) * (Math.sin(angleRad) * speed));

    frontLeftSpeed = powerCompY + powerCompX;
    frontRightSpeed = -powerCompY + powerCompX;
    rearLeftSpeed = powerCompY - powerCompX;
    rearRightSpeed = -powerCompY - powerCompX;

    // keep looping while we are still active, and BOTH motors are running.
    while (driveTime.seconds() < time) {
      this.motorFrontLeft(frontLeftSpeed);
      this.motorFrontRight(frontRightSpeed);
      this.motorRearLeft(rearLeftSpeed);
      this.motorRearRight(rearRightSpeed);

      // Display drive status for the driver.
      telemetry.addData(
        "Speed",
        "FL %5.2f:FR %5.2f:RL %5.2f:RR %5.2f",
        frontLeftSpeed,
        frontRightSpeed,
        rearLeftSpeed,
        rearRightSpeed);
      /*        telemetry.addData("Gyro", "Heading: " + this.gyroHeading() + " | IntZValue: " +
      imu.getgetIntegratedZValue());*/
      telemetry.addData("Gyro", "Heading: " + gyroHeading());
      // telemetry.update();
    }

    // Stop all motion;
    this.motorFrontLeft(0);
    this.motorFrontRight(0);
    this.motorRearLeft(0);
    this.motorRearRight(0);
  }

  double stepInputRotate(double dVal) {
    double stepVal = 0.0;
    double[] stepArray = {0.0, 0.15, 0.15, 0.2, 0.2, 0.25, 0.25, 0.3, 0.3, 0.35, 0.35};

    // get the corresponding index for the scaleInput array.
    int index = Math.abs((int) (dVal * 10.0));

    // index cannot exceed size of array minus 1.
    if (index > 10) {
      index = 10;
    }

    // get value from the array.
    if (dVal < 0) {
      stepVal = -stepArray[index];
    } else {
      stepVal = stepArray[index];
    }

    // return scaled value.
    return stepVal;
  }

  double stepInput(double dVal) {
    double stepVal = 0.0;
    double[] stepArray = {0.0, 0.2, 0.2, 0.25, 0.25, 0.33, 0.33, 0.44, 0.44, 0.56, 0.56};

    // get the corresponding index for the scaleInput array.
    int index = Math.abs((int) (dVal * 10.0));

    // index cannot exceed size of array minus 1.
    if (index > 10) {
      index = 10;
    }

    // get value from the array.
    if (dVal < 0) {
      stepVal = -stepArray[index];
    } else {
      stepVal = stepArray[index];
    }

    // return scaled value.
    return stepVal;
  }

  public void gyroHold(double speed, double angle, double holdTime) {

  }
}
