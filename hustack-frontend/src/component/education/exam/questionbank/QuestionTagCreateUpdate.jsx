import React, {useEffect, useState} from 'react';
import {
  CircularProgress,
  TextField,
} from "@mui/material";
import {request} from "../../../../api";
import {toast} from "react-toastify";
import {makeStyles} from "@material-ui/core/styles";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import PrimaryButton from "../../../button/PrimaryButton";
import TertiaryButton from "../../../button/TertiaryButton";

const useStyles = makeStyles((theme) => ({
  root: {
    "& .MuiTextField-root": {
      margin: theme.spacing(1),
      width: 200,
    },
  },
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120,
    maxWidth: 300,
  },
  dialogContent: {minWidth: 576},
}));
function QuestionTagCreateUpdate(props) {

  const classes = useStyles();

  const { open, setOpen, data, isCreate} = props;

  const [name, setName] = useState("")

  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (data?.name) {
      setName(data.name);
    }
  }, [data]);

  const handleSave = () =>{
    const body = {
      id: data?.id,
      name: name,
    }
    if(!validateBody(body)){
      return
    }

    setIsLoading(true)
    request(
      isCreate ? "post" : "put",
      '/exam-tag',
      (res) => {
        if(res.status === 200){
          if(res.data.resultCode === 200){
            toast.success(res.data.resultMsg)
            setIsLoading(false)
            closeDialog()
          }else{
            toast.error(res.data.resultMsg)
            setIsLoading(false)
          }
        }else {
          toast.error(res)
          setIsLoading(false)
        }
      },
      { onError: (e) => toast.error(e) },
      body
    );
  }
  const validateBody = (body) => {
    if(body.name == null || body.name === ''){
      toast.error('Tên tag không được bỏ trống')
      return false
    }
    return true
  }

  const closeDialog = () => {
    setOpen(false)
    setName("")
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        handleClose={closeDialog}
        classNames={{paper: classes.dialogContent}}
        title={`${isCreate ? 'Thêm mới Tag câu hỏi' : 'Cập nhật Tag câu hỏi'}`}
        content={
          <form className={classes.root} noValidate autoComplete="off">
            <div>
              <div>
                <TextField
                  autoFocus
                  required
                  id="questionTagName"
                  label="Tên tag"
                  placeholder="Nhập tên tag"
                  value={name}
                  style={{width: '100%'}}
                  size="small"
                  onChange={(event) => {
                    setName(event.target.value);
                  }}
                  InputLabelProps={{
                    shrink: true,
                  }}
                />
              </div>
            </div>
          </form>
        }
        actions={
          <div>
            <TertiaryButton
              variant="outlined"
              onClick={closeDialog}
            >
              Huỷ
            </TertiaryButton>
            <PrimaryButton
              disabled={isLoading}
              variant="contained"
              color="primary"
              style={{marginLeft: "15px"}}
              onClick={handleSave}
              type="submit"
            >
              {isLoading ? <CircularProgress/> : "Lưu"}
            </PrimaryButton>
          </div>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default QuestionTagCreateUpdate;
