import React from 'react';
import {request} from "../../../../api";
import {toast} from "react-toastify";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import TertiaryButton from "../../../button/TertiaryButton";
import PrimaryButton from "../../../button/PrimaryButton";

function QuestionBankDelete(props) {

  const { open, setOpen, id , onReloadQuestions} = props;

  const deleteQuestion = () =>{
    request(
      "delete",
      `/exam-question/${id}`,
      (res) => {
        if(res.data.resultCode === 200){
          onReloadQuestions()
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
        title="Xoá câu hỏi"
        content={
          <p style={{marginBottom: "30px"}}>Bạn có chắc chắn muốn xoá câu hỏi?</p>
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
              onClick={deleteQuestion}
            >
              Lưu
            </PrimaryButton>
          </div>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default QuestionBankDelete;
