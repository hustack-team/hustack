import React, {useEffect, useState} from 'react';
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {Error} from "@material-ui/icons";

function MyExamBlockScreenDialog(props) {

  const { open, setOpen, blockScreen } = props;

  const [countdown, setCountdown] = useState(blockScreen);

  useEffect(() => {
    if(open){
      setCountdown(blockScreen);
    }
  }, [open, blockScreen]);

  useEffect(() => {
    if (open && countdown > 0) {
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            closeDialog();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [open, countdown]);

  const closeDialog = () => {
    setOpen(false)
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        title={
          <div style={{display: 'flex', alignItems: 'center', color: 'red'}}>
            <Error/>
            <h3>Vi phạm quy chế thi</h3>
          </div>
        }
        content={
          <p style={{marginBottom: "30px", display: "flex", justifyContent: 'space-between'}}>
            Thí sinh cần chờ
            <strong style={{width: '25px', display: 'block', textAlign: 'center'}}>{countdown}</strong> giây
            để tiếp tục làm bài thi.
          </p>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default MyExamBlockScreenDialog;
