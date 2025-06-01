import React, {useCallback, useEffect, useRef, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {formatDateTime, formatDateTimeApi} from "../ultils/DateUltils";
import MyExamBlockScreenDialog from "./MyExamBlockScreenDialog";
import {request} from "../../../../api";
import {toast} from "react-toastify";
import * as faceapi from "face-api.js";
import {errorNoti} from "../../../../utils/notification";

function MyExamMonitor(props) {

  const {
    children,
    monitor,
    blockScreen,
    data,
    isCancel,
  } = props

  const [openBlockScreenDialog, setOpenBlockScreenDialog] = useState(false);
  const [messageBlockScreen, setMessageBlockScreen] = useState('');

  useEffect(() => {
    if(monitor && monitor > 0 && !data?.submitedAt){
      handleCheckingFocusTab();
      if(monitor === 2){
        startCameraMonitoring();
      }
    }
  }, []);

  useEffect(() => {
    if(isCancel){
      handleCancelCheckingFocusTab();
      if(monitor === 2){
        stopCameraMonitoring();
      }
    }
  }, [isCancel]);

  // ----------------------------------- Giám sát hành vi chuyển tab khi làm bài --------------------------
  const lastBlurTime = useRef(null);
  const eventQueue = useRef([]);
  const sendMonitorLog = async (logs) => {
    request(
      "post",
      '/exam-monitor',
      (res) => {},
      { onError: (e) => toast.error(e) },
      logs,
    );
  };
  // Debounce hàm gửi log để tránh spam request
  const debouncedSendLog = useRef(
    _.debounce((logs) => {
      sendMonitorLog(logs)
      eventQueue.current = []; // Xóa hàng đợi sau khi gửi
    }, 2000) // Chờ 2 giây trước khi gửi
  ).current;
  const onFocus = useCallback(() => {
    const currentTime = Date.now();
    if (lastBlurTime.current !== null) {
      const timeDiff = Math.round((currentTime - lastBlurTime.current) / 1000);
      console.log(`Tab is in focus. Time since last blur: ${timeDiff}ms. From ${formatDateTime(lastBlurTime.current)} to ${formatDateTime(currentTime)}`);

      eventQueue.current.push({
        examResultId: data?.examResultId,
        platform: 0,
        type: 0,
        startTime: formatDateTimeApi(lastBlurTime.current),
        toTime: formatDateTimeApi(currentTime),
        note: null,
      });
      debouncedSendLog(eventQueue.current);
    } else {
      console.log("Tab is in focus");
    }
    setOpenBlockScreenDialog(true);
    setMessageBlockScreen('Thí sinh đã rời khỏi màn hình thi.')
  }, [data?.examResultId]);

  const onBlur = useCallback(() => {
    lastBlurTime.current = Date.now();
    console.log("Tab is blurred");
  }, []);
  const handleCheckingFocusTab = () => {
    window.addEventListener("focus", onFocus);
    window.addEventListener("blur", onBlur);
    return () => {
      handleCancelCheckingFocusTab();
    };
  }
  const handleCancelCheckingFocusTab = () => {
    window.removeEventListener("focus", onFocus);
    window.removeEventListener("blur", onBlur);
  }

  // ----------------------------------- Giám sát hành vi qua camera khi làm bài --------------------------
  const videoRef = useRef(null);
  const timeoutRef = useRef(null);
  const prevDetectionsLength = useRef(1);
  const lastViolateTime = useRef(null)

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
      console.error('Lỗi khi tải model:', err);
    }
  };
  const startCameraMonitoring = async () => {
    try {
      await loadModels();
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      videoRef.current.srcObject = stream;
      await waitForVideo(videoRef.current);
      await monitorCamera();
    } catch (err) {
      if (err.name === 'NotAllowedError') {
        errorNoti("Quyền truy cập webcam bị từ chối. Vui lòng cấp quyền để bắt đầu giám sát.", true)
        return;
      } else {
        errorNoti("Không thể truy cập webcam. Vui lòng kiểm tra quyền truy cập hoặc thiết bị.", true)
      }
    }
  };
  const stopCameraMonitoring = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      videoRef.current.srcObject.getTracks().forEach(track => track.stop());
    }
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
  };
  const monitorCamera = async () => {
    if (!videoRef.current) return;

    const detections = await faceapi.detectAllFaces(
      videoRef.current,
      new faceapi.TinyFaceDetectorOptions({
        inputSize: 224, // Kích thước đầu vào nhỏ hơn để tăng tốc độ
        scoreThreshold: 0.5 // Ngưỡng phát hiện
      })
    ).withFaceLandmarks();

    const currentTime = Date.now();
    if (detections.length !== prevDetectionsLength.current) {
      if(detections.length < 1){
        lastViolateTime.current = currentTime
      }else if(detections.length === 1){
        const timeDiff = Math.round((currentTime - lastViolateTime.current) / 1000);
        if(timeDiff > 0){
          if(prevDetectionsLength.current < 1){
            console.log(`Sinh viên rời camera ${timeDiff} giây, rời từ ${formatDateTime(lastViolateTime.current)} đến ${formatDateTime(currentTime)}`)
            setMessageBlockScreen(`Sinh viên rời camera ${timeDiff} giây.`)
            eventQueue.current.push({
              examResultId: data?.examResultId,
              platform: 1,
              type: 1,
              startTime: formatDateTimeApi(lastViolateTime.current),
              toTime: formatDateTimeApi(currentTime),
              note: null,
            });
          }else if(prevDetectionsLength.current > 1){
            console.log(`Có nhiều hơn 1 sinh viên làm bài thi ${timeDiff} giây, rời từ ${formatDateTime(lastViolateTime.current)} đến ${formatDateTime(currentTime)}`)
            setMessageBlockScreen(`Có nhiều hơn 1 sinh viên làm bài thi ${timeDiff} giây`)
            eventQueue.current.push({
              examResultId: data?.examResultId,
              platform: 1,
              type: 2,
              startTime: formatDateTimeApi(lastViolateTime.current),
              toTime: formatDateTimeApi(currentTime),
              note: null,
            });
          }else{
            console.log('Sinh viên đang sử dụng camera',formatDateTime(currentTime))
            setMessageBlockScreen('')
          }
          debouncedSendLog(eventQueue.current);
          setOpenBlockScreenDialog(true);
        }
      }else{
        lastViolateTime.current = currentTime
      }
      prevDetectionsLength.current = detections.length;
    }

    if (videoRef.current) {
      timeoutRef.current = setTimeout(monitorCamera, 100);
    }
  };

  return (
    <div style={{position: 'relative'}}>
      {children}
      <MyExamBlockScreenDialog
        open={openBlockScreenDialog}
        setOpen={setOpenBlockScreenDialog}
        blockScreen={blockScreen}
        description={messageBlockScreen}
      />
      <video ref={videoRef} autoPlay playsInline
             style={{width: '300px', position: 'absolute', top: '0', left: '0', borderRadius: '5px'}}/>
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
export default withScreenSecurity(MyExamMonitor, screenName, true);
//export default MyExam;
