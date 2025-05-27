import React, {useCallback, useEffect, useRef, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {formatDateTime, formatDateTimeApi} from "../ultils/DateUltils";
import MyExamBlockScreenDialog from "./MyExamBlockScreenDialog";
import {request} from "../../../../api";
import {toast} from "react-toastify";

function MyExamMonitor(props) {

  const {
    children,
    monitor,
    blockScreen,
    data,
    isCancel,
  } = props

  const [openBlockScreenDialog, setOpenBlockScreenDialog] = useState(false);

  useEffect(() => {
    if(monitor && monitor > 0 && !data?.submitedAt){
      return handleCheckingFocusTab();
    }
  }, []);

  useEffect(() => {
    console.log('isCancel',isCancel)
    if(isCancel){
      console.log('Xoá này')
      window.removeEventListener("focus", onFocus);
      window.removeEventListener("blur", onBlur);
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
      console.log('logs',logs)
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
  }, [data?.examResultId]);

  const onBlur = useCallback(() => {
    lastBlurTime.current = Date.now();
    console.log("Tab is blurred");
  }, []);
  const handleCheckingFocusTab = () => {
    window.addEventListener("focus", onFocus);
    window.addEventListener("blur", onBlur);
    return () => {
      window.removeEventListener("focus", onFocus);
      window.removeEventListener("blur", onBlur);
    };
  }

  return (
    <div>
      {children}
      <MyExamBlockScreenDialog
        open={openBlockScreenDialog}
        setOpen={setOpenBlockScreenDialog}
        blockScreen={blockScreen}
        description={'Thí sinh đã rời khỏi màn hình thi.'}
      />
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
export default withScreenSecurity(MyExamMonitor, screenName, true);
//export default MyExam;
