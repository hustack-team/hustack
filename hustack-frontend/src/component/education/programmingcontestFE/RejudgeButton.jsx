import ReplayIcon from "@mui/icons-material/Replay";
import {IconButton, Tooltip} from "@mui/material";
import {request} from "api";
import {errorNoti, infoNoti} from "utils/notification";
import {useTranslation} from "react-i18next";

export const RejudgeButton = ({submissionId}) => {
  const {t} = useTranslation(["education/programmingcontest/contest", "common"]);

  const handleRejudge = (submissionId) => {
    request(
      "post",
      "/submissions/" + submissionId + "/evaluation",
      (res) => {
        infoNoti(
          `${t('education/programmingcontest/contest:reJudgingNotification')} ${submissionId.substring(0, 6)}`
        );
      }, {
        onError: (e) => {
          setLoading(false);
          errorNoti(t("common:error", 3000))
        }
      });
  };

  return (
    <Tooltip title={t('education/programmingcontest/contest:reJudge')}>
      <IconButton
        variant="contained"
        color="primary"
        onClick={() => {
          handleRejudge(submissionId);
        }}
      >
        <ReplayIcon/>
      </IconButton>
    </Tooltip>
  );
};
