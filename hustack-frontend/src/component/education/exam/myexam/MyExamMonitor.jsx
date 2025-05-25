import React, {useEffect, useRef, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {formatDateTime} from "../ultils/DateUltils";
import MyExamBlockScreenDialog from "./MyExamBlockScreenDialog";

function MyExamMonitor(props) {

  const {
    children,
    monitor,
    blockScreen,
  } = props

  const [openBlockScreenDialog, setOpenBlockScreenDialog] = useState(false);

  useEffect(() => {
    if(monitor === 1 || monitor === 2){
      handleCheckingFocusTab()
    }
  }, []);

  // ----------------------------------- Giám sát hành vi chuyển tab khi làm bài --------------------------
  const lastBlurTime = useRef(null);
  const onFocus = () => {
    const currentTime = Date.now();
    if (lastBlurTime.current !== null) {
      const timeDiff = currentTime - lastBlurTime.current;
      console.log(`Tab is in focus. Time since last blur: ${timeDiff}ms. From ${formatDateTime(lastBlurTime.current)} to ${formatDateTime(currentTime)}`);
    } else {
      console.log("Tab is in focus");
    }
    setOpenBlockScreenDialog(true)
  };
  const onBlur = () => {
    lastBlurTime.current = Date.now();
    console.log("Tab is blurred");
  };
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
      />
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
export default withScreenSecurity(MyExamMonitor, screenName, true);
//export default MyExam;
