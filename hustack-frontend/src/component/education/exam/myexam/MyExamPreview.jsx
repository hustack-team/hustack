import React, {useRef, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Card, CardActions,
  CardContent, CircularProgress,
} from "@material-ui/core";
import {request} from "../../../../api";
import {useHistory} from "react-router-dom";
import {toast} from "react-toastify";
import {formatDateTime} from "../ultils/DateUltils";
import {parseHTMLToString} from "../ultils/DataUltils";
import {useLocation} from "react-router";
import TertiaryButton from "../../../button/TertiaryButton";
import PrimaryButton from "../../../button/PrimaryButton";
import {useMenu} from "../../../../layout/sidebar/context/MenuContext";
import * as faceapi from "face-api.js";
import {errorNoti} from "../../../../utils/notification";

function MyExamPreview(props) {

  const history = useHistory();
  const location = useLocation();
  const exam = location.state?.exam
  const test = location.state?.test
  const { closeMenu, openMenu } = useMenu();

  if(test === undefined){
    window.location.href = '/exam/my-exam';
  }

  const [isLoading, setIsLoading] = useState(false);

  const handleDoingExam = () => {
    setIsLoading(true)
    request(
      "get",
      `/exam/student/submissions/${test?.examStudentTestId}/attempts`,
      (res1) => {
        if(res1.status === 200){
          if(res1.data.resultCode === 200){
            request(
              "get",
              `/exam/student/submissions/examStudentTest/${test?.examStudentTestId}`,
              (res) => {
                if(res.status === 200){
                  setIsLoading(false)
                  if(res.data.resultCode === 200){
                    stopCamera()
                    history.push({
                      pathname: `/exam/doing`,
                      state: {
                        data: res.data.data,
                      },
                    });
                  }else{
                    toast.error(res.data.resultMsg)
                  }
                }else {
                  toast.error(res)
                  setIsLoading(false)
                }
              },
              { onError: (e) => toast.error(e) },
            );
          }else{
            toast.error(res1.data.resultMsg)
            setIsLoading(false)
          }
          closeMenu()
        }else {
          toast.error(res1)
          setIsLoading(false)
          closeMenu()
        }
      },
      { onError: (e) => toast.error(e) },
    );
  };

  // Bật camera
  const videoRef = useRef(null);
  const [isCameraOn, setIsCameraOn] = useState(false);
  const waitForVideo = (videoElement) => {
    return new Promise((resolve, reject) => {
      if (videoElement.readyState >= 2) {
        resolve();
      } else {
        videoElement.onloadedmetadata = () => resolve();
        videoElement.onerror = () => reject(errorNoti("Không thể tải video từ webcam", true));
      }
    });
  };
  const loadModels = async () => {
    try {
      await Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri('/models'),
        faceapi.nets.faceLandmark68Net.loadFromUri('/models')
      ]);
      console.log('Đã tải model face-api.js');
    } catch (err) {
      errorNoti(`Lỗi khi tải model: ${err}`, true)
    }
  };
  const startCamera = async () => {
    try {
      await loadModels();
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      videoRef.current.srcObject = stream;
      await waitForVideo(videoRef.current);
      setIsCameraOn(true);
    } catch (err) {
      if (err.name === 'NotAllowedError') {
        errorNoti("Quyền truy cập webcam bị từ chối. Vui lòng cấp quyền để bắt đầu giám sát.", true)
      } else {
        errorNoti("Không thể truy cập webcam. Vui lòng kiểm tra quyền truy cập hoặc thiết bị.", true)
      }
      return
    }
  };
  const stopCamera = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      videoRef.current.srcObject.getTracks().forEach(track => track.stop());
    }
    setIsCameraOn(false);
  };

  return (
    <div>
      <Card elevation={5} >
        <CardContent>
          <div style={{display: "flex", flexDirection: "column", alignItems: 'center', width: '100%'}}>
            <h1 style={{margin: 0, padding: 0}}>{exam?.examName}</h1>
            <p style={{margin: 0, padding: 0}}>{parseHTMLToString(exam?.examDescription)}</p>
            <div style={{display: "flex"}}>
              <p style={{margin: '0 20px 0 0', padding: 0, display: "flex"}}><span style={{
                fontWeight: "bold",
                marginRight: '5px'
              }}>Thời gian bắt đầu:</span>{formatDateTime(exam?.startTime)}</p>
              <p style={{margin: 0, padding: 0, display: "flex"}}><span style={{
                fontWeight: "bold",
                marginRight: '5px'
              }}>Thời gian kết thúc:</span>{formatDateTime(exam?.endTime)}</p>
            </div>
          </div>

          <div>
            <h3>* Nội quy thi:</h3>
            <ol style={{fontSize: "15px", paddingLeft: '28px'}}>
              <li style={{marginBottom: '5px'}}>Bài thi chỉ được làm 01 lần duy nhất, làm bài trong thời gian quy
                định.
              </li>
              <li style={{marginBottom: '5px'}}>Không được sử dụng tài liêu, thiết bị điện tử gian lận thi.</li>
              <li style={{marginBottom: '5px'}}>Các thao tác thi đều được giám sát trực tiếp, mọi hành vi thoát khỏi màn
                hình / tab làm bài đều được coi là gian lận và vi phạm quy chế thi.
              </li>
              {
                exam?.examMonitor === 2 && (
                  <li style={{marginBottom: '5px'}}>Cho phép quyền <span
                    style={{fontWeight: "bold", textDecoration: 'underline', cursor: 'pointer', color: 'blue'}}
                    onClick={startCamera}>truy cập camera</span> để thực hiện giám sát (Thí sinh nên thi trong
                    môi trường đầy đủ ánh sáng).</li>
                )
              }
              {
                exam?.examBlockScreen > 0 && (
                  <li style={{marginBottom: '5px'}}>Mỗi lần vi phạm thí sinh sẽ bị tạm dừng
                    thi <strong>{exam?.examBlockScreen}</strong> giây.</li>
                )
              }
            </ol>
          </div>

          <div style={{textAlign: 'center'}}>
            <PrimaryButton
              disabled={isLoading || (exam?.examMonitor === 2 && !isCameraOn)}
              variant="contained"
              color="primary"
              onClick={handleDoingExam}
            >
              {isLoading ? <CircularProgress/> : "Bắt đầu làm bài"}
            </PrimaryButton>
          </div>

          <video ref={videoRef} autoPlay playsInline style={{width: '300px', position: 'fixed', top: '88px', right: '24px', borderRadius: '5px'}}/>
        </CardContent>
        <CardActions style={{justifyContent: 'flex-end'}}>
        <TertiaryButton
            variant="outlined"
            onClick={() => {
              history.push("/exam/my-exam")
              openMenu()
            }}
          >
            Hủy
          </TertiaryButton>
        </CardActions>
      </Card>
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
export default withScreenSecurity(MyExamPreview, screenName, true);
//export default MyExam;
