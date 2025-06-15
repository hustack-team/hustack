import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Box,
  TextField, Typography
} from "@material-ui/core";
import {request} from "../../../../api";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import TertiaryButton from "../../../button/TertiaryButton";
import PrimaryButton from "../../../button/PrimaryButton";
import {errorNoti, successNoti} from "../../../../utils/notification";
import {formatDateTime, getDiffMinutes, getDiffTimeFormatted} from "../ultils/DateUltils";

function ExamContinueDoingDialog(props) {

  const { open, setOpen, onReload , examStudentTest} = props;

  const [extraTime, setExtraTime] = useState(null);

  const handleUpdateExamResult = () =>{
    const body = {
      examStudentTestId: examStudentTest?.examStudentTestId,
      submitAgain: true,
      extraTime,
    }
    request(
      "put",
      `/exam-result`,
      (res) => {
        if(res.data.resultCode === 200){
          successNoti(res.data.resultMsg, 3000)
          onReload()
          closeDialog()
        }else{
          errorNoti(res.data.resultMsg, 3000)
        }
      },
      { onError: (e) => errorNoti(e, 3000) },
      body,
    );
  }

  const closeDialog = () => {
    setOpen(false)
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        handleClose={closeDialog}
        title="Mở lại bài thi cho học viên tiếp tục làm"
        content={
          <div>
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" gutterBottom>
                Thí sinh đã thi <strong> {getDiffMinutes(examStudentTest?.startedAt,examStudentTest?.updatedAt)} phút</strong> bắt đầu từ
                <strong> {formatDateTime(examStudentTest?.startedAt)}</strong> đến lần cập nhật cuối cùng
                <strong> {formatDateTime(examStudentTest?.updatedAt)}</strong>.
              </Typography>
              <Typography style={{ marginTop: "24px"}}>
                Bấm <strong> Lưu </strong> để cho phép thí sinh tiếp tục làm bài.
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', fontStyle: 'italic'}}>
                <Typography variant="body2" color="text.secondary" sx={{ mr: 1 }}>
                 * Giảng viên có thể mở thêm
                </Typography>
                <TextField
                  type="number"
                  id="questionCode"
                  value={extraTime}
                  style={{ width: "50px", margin: "0 4px", fontStyle: 'initial'}}
                  size="small"
                  onChange={(e) => setExtraTime(e.target.value)}
                  InputLabelProps={{
                    shrink: true,
                  }}
                  InputProps={{
                    type: "number",
                    sx: {
                      '& input::-webkit-outer-spin-button, & input::-webkit-inner-spin-button': {
                        display: 'none'
                      },
                      '& input[type=number]': {
                        MozAppearance: 'textfield'
                      },
                    }
                  }}
                />
                <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                  phút cho thí sinh.
                </Typography>
              </Box>
            </Box>
          </div>
        }
        actions={
          <div>
            <TertiaryButton
              variant="outlined"
              onClick={closeDialog}
            >
              Hủy
            </TertiaryButton>
            <PrimaryButton
              variant="contained"
              color="primary"
              style={{marginLeft: "15px"}}
              onClick={handleUpdateExamResult}
            >
              Lưu
            </PrimaryButton>
          </div>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAM_MANAGEMENT";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default ExamContinueDoingDialog;
