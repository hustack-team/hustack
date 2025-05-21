import HustModal from "component/common/HustModal";
import React, {useState, useEffect} from "react";
import {MenuItem, TextField} from "@mui/material";
import {useTranslation} from "react-i18next";
import {saveProblemToContest} from "./service/ContestProblemService";
import {
  getSubmissionModeFromConstant,
  SUBMISSION_MODE_NOT_ALLOWED,
  SUBMISSION_MODE_SOLUTION_OUTPUT,
  SUBMISSION_MODE_SOURCE_CODE,
  SUBMISSION_MODE_HIDDEN
} from "./Constant";
import { request } from "api";
import { errorNoti } from "utils/notification";

const ModalUpdateProblemInfoInContest = (props) => {
  const {contestId, editingProblem, isOpen, handleSuccess, handleClose} = props;

  const {t} = useTranslation(
    ["education/programmingcontest/problem", "common", "validation"]
  );

  const [problemRename, setProblemRename] = useState("");
  const [problemRecode, setProblemRecode] = useState("");
  const [submissionMode, setSubmissionMode] = useState(SUBMISSION_MODE_SOURCE_CODE)
  const [loading, setLoading] = useState(false);
  const [forbiddenInstructions, setForbiddenInstructions] = useState("");
  const [coefficientPoint, setCoefficientPoint] = useState(1);
  const [canEditCoefficientPoint, setCanEditCoefficientPoint] = useState("Y");

  useEffect(() => {
    if (isOpen && contestId) {
      setLoading(true);
      request(
        "get",
        `/contests/${contestId}`,
        (res) => {
          setLoading(false);
          const data = res.data;
          const canEdit = data.canEditCoefficientPoint || "Y";
          setCanEditCoefficientPoint(canEdit);
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

  useEffect(() =>  {
    setProblemRename(editingProblem?.problemRename || "");
    setProblemRecode(editingProblem?.problemRecode || "");
    setForbiddenInstructions(editingProblem?.forbiddenInstructions||"");
    setSubmissionMode(editingProblem?.submissionMode || SUBMISSION_MODE_SOURCE_CODE);
    setCoefficientPoint(editingProblem?.coefficientPoint ?? 1);
  }, [isOpen, editingProblem]);

  const handleAddProblemToContest = () => {
    let body = {
      contestId: contestId,
      problemId: editingProblem.problemId,
      problemName: editingProblem.problemName,
      problemRename: problemRename,
      problemRecode: problemRecode,
      submissionMode: submissionMode,
      forbiddenInstructions: forbiddenInstructions,
      coefficientPoint: canEditCoefficientPoint === "N" ? (editingProblem?.coefficientPoint ?? 1) : coefficientPoint,
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
    setForbiddenInstructions("");
    setSubmissionMode(SUBMISSION_MODE_SOURCE_CODE);
    setCoefficientPoint(canEditCoefficientPoint === "N" ? (editingProblem?.coefficientPoint ?? 1) : 1);
  }

  return (
    <HustModal
      open={isOpen}
      onOk={handleAddProblemToContest}
      textOk={t("common:save")}
      onClose={handleClose}
      isLoading={loading}
      title={t("common:edit", { name: "problem" })}
    >
      <TextField
        fullWidth
        required
        disabled
        label={"Problem"}
        value={editingProblem?.problemName}
      />
      <TextField
        fullWidth
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
        label={"Forbidden instructions"}
        placeholder={"If this field is left blank, a default value will be generated"}
        value={forbiddenInstructions}
        onChange={(event) => {
          setForbiddenInstructions(event.target.value);
        }}
        sx={{marginTop: "16px"}}
      />

      <TextField
        fullWidth
        autoFocus
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
        <MenuItem value={SUBMISSION_MODE_HIDDEN}>
          {getSubmissionModeFromConstant(SUBMISSION_MODE_HIDDEN)}
        </MenuItem>

      </TextField>
      <TextField
        fullWidth
        required
        type="number"
        label={"Score Coefficient (Min: 1, Max: 100)"}
        placeholder={"Enter a positive integer from 1 to 100 (default is 1)"}
        value={coefficientPoint}
        onChange={(event) => {
          if (canEditCoefficientPoint === "Y") {
            const value = parseInt(event.target.value);
            if ((value >= 1 && value <= 100) || isNaN(value)) {
              setCoefficientPoint(value || 1);
            }
          }
        }}
        inputProps={{ min: 1, max: 100 }}
        disabled={canEditCoefficientPoint === "N"}
        sx={{ marginTop: "16px" }}
      />
    </HustModal>
  );
}

export default React.memo(ModalUpdateProblemInfoInContest);