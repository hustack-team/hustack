import React, {useState} from "react";
import {useHistory, useParams} from "react-router-dom";
import {Grid, TextField} from "@mui/material";
import {request} from "../../../api";
import {LoadingButton} from "@mui/lab";
import ProgrammingContestLayout from "../programmingcontestFE/ProgrammingContestLayout";
import Box from "@mui/material/Box";
import {errorNoti} from "../../../utils/notification";
import {useTranslation} from "react-i18next";

export default function StudentQuizDetailCheckAndConfirmGroupCode() {
  const {testId} = useParams();
  const history = useHistory();
  const {t} = useTranslation(["common"]);
  const [groupCode, setGroupCode] = useState("");

  const [requestFailed, setRequestFailed] = useState(false);
  const [loading, setLoading] = useState(false);

  function onConfirmCode() {
    setLoading(true);
    request(
      "get",
      "/confirm-update-group-code-quiz-test/" + testId + "/" + groupCode,
      (res) => {
        alert("update " + res.data);
        setLoading(false);
        history.push("/edu/class/student/quiztest/detail/" + testId);
        //setOpen(false);
      },
      {
        onError: e => {
          setLoading(false);
          errorNoti(t("common:error", 3000))
        },
        406: () => {
          //setMessageRequest("Time Out!");
          setRequestFailed(true);
        },
      }
    );
  }

  const handleExit = () => {
    history.push(`/edu/class/student/quiztest/detail/${testId}`);
  }

  return (
    <ProgrammingContestLayout onBack={handleExit}>
      <Grid container spacing={2} mt={0}>
        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            autoFocus
            required
            id="Code"
            label="Code"
            value={groupCode}
            placeholder="Code"
            onChange={(event) => {
              setGroupCode(event.target.value);
            }}
          />
        </Grid>
      </Grid>

      <Box width="100%" sx={{mt: "20px"}}>
        <LoadingButton
          variant="contained"
          onClick={onConfirmCode}
          loading={loading}
          sx={{textTransform: "none",}}
        >
          Confirm
        </LoadingButton>
      </Box>
    </ProgrammingContestLayout>
  );
}
