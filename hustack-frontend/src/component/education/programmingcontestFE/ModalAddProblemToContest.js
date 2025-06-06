// import HustModal from "component/common/HustModal";
import {HustModal} from "erp-hust/lib/HustModal";
import React, {useState, useEffect} from "react";
import {MenuItem, TextField} from "@mui/material";
import {useTranslation} from "react-i18next";
import {saveProblemToContest} from "./service/ContestProblemService";
import {
  getSubmissionModeFromConstant,
  SUBMISSION_MODE_NOT_ALLOWED,
  SUBMISSION_MODE_SOLUTION_OUTPUT,
  SUBMISSION_MODE_SOURCE_CODE
} from "./Constant";
import { request } from "api"; 
import { errorNoti } from "utils/notification"; 

const ModalAddProblemToContest = (props) => {
  const {contestId, chosenProblem, isOpen, handleSuccess, handleClose} = props;

  const {t} = useTranslation(
    ["education/programmingcontest/problem", "common", "validation"]
  );

  const [problemRename, setProblemRename] = useState("");
  const [problemRecode, setProblemRecode] = useState("");
  const [submissionMode, setSubmissionMode] = useState(SUBMISSION_MODE_SOURCE_CODE)
  const [loading, setLoading] = useState(false);
  const [coefficientPoint, setCoefficientPoint] = useState(1);
  const [forbiddenInstructions, setForbiddenInstructions] = useState("");
  const [canEditCoefficientPoint, setCanEditCoefficientPoint] = useState(1); 

  useEffect(() => {
    if (isOpen && contestId) {
      setLoading(true);
      request(
        "get",
        `/contests/${contestId}`,
        (res) => {
          setLoading(false);
          const data = res.data;
          const canEdit = data.canEditCoefficientPoint ?? 0;
          setCanEditCoefficientPoint(canEdit);
          if (canEdit === 0) {
            setCoefficientPoint(1); 
          }
        },
        {
          onError: () => {
            errorNoti(t("error", { ns: "common" }), 3000);
            setLoading(false);
          },
        }
      );
    }
  }, [isOpen, contestId, t]);

  const handleAddProblemToContest = () => {
    let body = {
      contestId: contestId,
      problemId: chosenProblem.problemId,
      problemName: chosenProblem.problemName,
      problemRename: problemRename,
      problemRecode: problemRecode,
      submissionMode: submissionMode,
      forbiddenInstructions: forbiddenInstructions,
      coefficientPoint: canEditCoefficientPoint === 0 ? 1 : coefficientPoint, 
    };

    setLoading(true);

    saveProblemToContest(
      body,
      handleSuccess,
      () => {
        setLoading(false);
        handleClose();
        resetField();
      },
    )
  }

  const resetField= () => {
    setProblemRename("");
    setProblemRecode("");
    setCoefficientPoint(canEditCoefficientPoint === 0 ? 1 : 1); 
    setSubmissionMode(SUBMISSION_MODE_SOURCE_CODE);
    setForbiddenInstructions("");
  }

  return (
    <HustModal
      open={isOpen}
      onOk={handleAddProblemToContest}
      textOk={t("common:save")}
      onClose={handleClose}
      isLoading={loading}
      title={t("common:create", { name: "problem" })}
    >
      <TextField
        fullWidth
        required
        disabled
        label={"Problem"}
        value={chosenProblem?.problemName}
      />
      <TextField
        fullWidth
        autoFocus
        label={"Problem name in this contest"}
        placeholder={"If this field is left blank, the original problem name will be taken"}
        value={problemRename}
        onChange={(event) => {
          setProblemRename(event.target.value);
        }}
        sx={{marginTop: "16px"}}
      />
      <TextField
        fullWidth
        label={"Problem code in this contest"}
        placeholder={"If this field is left blank, a default value will be generated"}
        value={problemRecode}
        onChange={(event) => {
          setProblemRecode(event.target.value);
        }}
        sx={{marginTop: "16px"}}
      />
      <TextField
        fullWidth
        select
        id="Submission Mode"
        label="Submission Mode"
        onChange={(event) => {
          setSubmissionMode(event.target.value);
        }}
        value={submissionMode}
        sx={{marginTop: "16px"}}
      >
        <MenuItem value={SUBMISSION_MODE_SOURCE_CODE}>
          {getSubmissionModeFromConstant(SUBMISSION_MODE_SOURCE_CODE)}
        </MenuItem>
        <MenuItem value={SUBMISSION_MODE_SOLUTION_OUTPUT}>
          {getSubmissionModeFromConstant(SUBMISSION_MODE_SOLUTION_OUTPUT)}
        </MenuItem>
        <MenuItem value={SUBMISSION_MODE_NOT_ALLOWED}>
          {getSubmissionModeFromConstant(SUBMISSION_MODE_NOT_ALLOWED)}
        </MenuItem>
      </TextField>
      <TextField
        fullWidth
        required
        type="number"
        label={t("common:scoreCoefficientTitle")}
        placeholder={t("common:scoreCoefficientInput")}
        value={coefficientPoint}
        onChange={(event) => {
          if (canEditCoefficientPoint === 1) {
            const value = parseInt(event.target.value);
            if ((value >= 1 && value <= 100) || isNaN(value)) {
              setCoefficientPoint(value || 1);
            }
          }
        }}
        inputProps={{ min: 1, max: 100 }}
        disabled={canEditCoefficientPoint === 0}
        sx={{ marginTop: "16px" }}
      />
    </HustModal>
  );
}

export default React.memo(ModalAddProblemToContest);