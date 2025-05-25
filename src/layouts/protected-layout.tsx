import LoadingPage from "@pages/loading";
import { useToken } from "@state/token";
import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router";

export default function AuthenticatedLayout() {
  const navigate = useNavigate();
  const token = useToken((state) => state.token);
  const [isAuth, setIsAuth] = useState(false);

  useEffect(() => {
    let tokenExpTime = token.issueAtMS + token.expiresInMS;
    let nowTime = Date.now();

    if (!token.accessToken) {
      navigate("/login");
    } else if (nowTime > tokenExpTime) {
      navigate("/refresh");
    } else {
      setIsAuth(true);
    }
  }, [token]);

  return <>{isAuth ? <Outlet /> : <LoadingPage />}</>;
}
