import * as React from "react";
import {useTranslation} from "react-i18next";
import {a11yProps, AntTab, AntTabs, TabPanel} from "component/tab";
import StudentViewProblemList from "./StudentViewProblemList";
import StudentViewSubmission from "./StudentViewSubmission";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import {useHistory} from "react-router-dom";

export default function StudentViewContestDetail() {
  const {t} = useTranslation(
    "education/programmingcontest/studentviewcontestdetail"
  );
  const history = useHistory();

  const [selectedTab, setSelectedTab] = React.useState(0);

  const handleChangeTab = (event, newTabValue) => {
    setSelectedTab(newTabValue);
  };

  const handleExit = () => {
    history.push(`/programming-contest/student-list-contest-registered`);
  }

  return (
    <ProgrammingContestLayout title={""} onBack={handleExit}>
      <AntTabs
        value={selectedTab}
        onChange={handleChangeTab}
        aria-label="contest detail tabs"
      >
        <AntTab label={t("problemList.title")} {...a11yProps(0)} />
        <AntTab label={t("submissionList.title")} {...a11yProps(1)} />
      </AntTabs>

      <TabPanel value={selectedTab} index={0} dir={"ltr"}>
        <StudentViewProblemList />
      </TabPanel>
      <TabPanel value={selectedTab} index={1} dir={"ltr"}>
        <StudentViewSubmission />
      </TabPanel>
    </ProgrammingContestLayout>
  );
}
