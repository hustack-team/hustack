import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Input
} from "@material-ui/core";
import {request} from "../../../../api";
import {toast} from "react-toastify";
import {DialogActions} from "@mui/material";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import TertiaryButton from "../../../button/TertiaryButton";
import PrimaryButton from "../../../button/PrimaryButton";

function ExamDelete(props) {

  const { open, setOpen, id , onReload} = props;

  const handleDelete = () =>{
    request(
      "delete",
      `/exam/${id}`,
      (res) => {
        if(res.data.resultCode === 200){
          onReload()
          toast.success(res.data.resultMsg)
          setOpen(false)
        }else{
          toast.error(res.data.resultMsg)
        }
      },
      { onError: (e) => toast.error(e) },
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
        title="Xoá kỳ thi"
        content={
          <p style={{marginBottom: "30px"}}>Bạn có chắc chắn muốn xoá kỳ thi?</p>
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
              onClick={handleDelete}
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
export default ExamDelete;
