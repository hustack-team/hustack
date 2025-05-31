import withScreenSecurity from "component/withScreenSecurity";
import { Paper } from "@mui/material";
import TeacherListGroup from "./ListTeacherGroupByMembership";

function ListContestManager() {
  return (
      <TeacherListGroup />
  );
}

const screenName = "SCR_MANAGER_CONTEST_LIST";
export default withScreenSecurity(ListContestManager, screenName, true);