package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;

import javax.xml.datatype.Duration;

@Autonomous(name = "TTAutoGrabBaseplateBlue", group = "TT")
public class TTAutoGrabBaseplateBlue extends LinearOpMode {

  // States
  private enum AutoState {
    INITIALIZE,
    GO_TO_PLATE,
    GRAB_PLATE,
    BRING_PLATE,
    LET_GO,
    GO_TO_LINE,
    STOP
  }

  private AutoState currentState = AutoState.INITIALIZE;
  private ElapsedTime runtime = new ElapsedTime();
  private ElapsedTime timer = new ElapsedTime();
  private TTRobot robot;

  @Override
  public void runOpMode() {
    double x = 0.0, y = 0.0, z = 0.0;

    /*
     * Initialize the standard drive system variables.
     * The init() method of the hardware class does most of the work here
     */
    robot = new TTRobot();
    robot.init(hardwareMap, telemetry);

    sleep(2000);
    // start calibrating the gyro.
    telemetry.addData(">", "Gyro Calibrating. Do Not move!");
    // robot.calibrate();
    // telemetry.addData(">", "Robot Heading = %d", robot.gyroHeading());

    // Put vuforia Here

    waitForStart();
    /*
    if (tfod != null) {
        tfod.deactivate();
    }
    */
    // run until the end of the match (driver presses STOP)
    while (opModeIsActive()) {
      telemetry.addData("Status", "Run Time: " + runtime.toString());

      switch (currentState) {
        case INITIALIZE:
          telemetry.addData("state", currentState.toString());
          runtime.reset();
          /*
          if (skystonepos.equals(SkyStonePos.UNKNOWN) && tfod != null) {
              tfod.activate();
          }
          */
          currentState = AutoState.GO_TO_PLATE;
          robot.timeDrive(0.5, 1, 90);
          robot.timeDrive(0.5, 0.2, 270);
          break;
        case GO_TO_PLATE:
          telemetry.addData("state", currentState.toString()); // make sure

          robot.timeDrive(0.5, 2.4, 180);
          currentState = AutoState.GRAB_PLATE;
          break;
        case GRAB_PLATE:
          telemetry.addData("state", currentState.toString());

          robot.bpGrabber(1);
          robot.timeDrive(0, 1, 0);
          currentState = AutoState.BRING_PLATE;
          break;
        case BRING_PLATE:
          telemetry.addData("state", currentState.toString());
          robot.bpGrabber(1);
          robot.timeDrive(0.5, 3, 0); // may need to bve tweaked according to testing
          currentState = AutoState.LET_GO;
          break;
        case LET_GO:
          telemetry.addData("state", currentState.toString());
          robot.bpGrabber(0);

          currentState = AutoState.GO_TO_LINE;
          robot.timeDrive(0.5, 2, 270);
          break;
        case GO_TO_LINE:
          telemetry.addData("state", currentState.toString());
          // if(
          robot.driveToLine(0.5, 270); // ){
          currentState = AutoState.STOP;
          // }

          // distToLine(x, y, z);
          break;
        case STOP:
          telemetry.addData("state", currentState.toString());

          stop();
          break;

        default:
          telemetry.addData("state", currentState.toString());

          stop();
          break;
      }
      telemetry.update();
    }
  }
}
