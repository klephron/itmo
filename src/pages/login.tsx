import { useState } from "react";
import { useClient } from "@state/client";
import { useUser } from "@state/user";
import { useNavigate } from "react-router";

import { useToken } from "@state/token";

import styles from "./login.module.scss";

export default function LoginPage() {
  const navigate = useNavigate();

  const client = useClient((state) => state.client);

  const setUser = useUser((state) => state.update);
  const setToken = useToken((state) => state.update);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function login(e: React.MouseEvent<HTMLButtonElement, MouseEvent>) {
    e.preventDefault();

    client.auth
      .login(
        {
          username: username,
          password: password,
        },
        { secure: false }
      )
      .then((r) =>
        r.json().then((data) => {
          try {
            setUser({
              id: Number(data["id"]),
              username: data["username"],
              present: true,
            });

            setToken({
              accessToken: data["accessToken"],
              refreshToken: data["refreshToken"],
              tokenType: "Bearer",
              issueAtMS: Number(data["iat"]),
              expiresInMS: Number(data["expiresIn"]) * 1000,
              present: true,
            });
            navigate("/");
          } catch (e) {
            console.error(e);
          }
        })
      )
      .catch(async (e) => {
        switch (e.status) {
          case 401:
            alert("Login failed");
            break;
          default:
            alert(await e);
        }
      });
  }

  return (
    <>
      <div className={styles.root}>
        <form>
          <div className={styles.title}>
            <h2>ITMO PAI</h2>
          </div>
          <div className={styles.content}>
            <label>Username:</label>
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} />
            <label>Password:</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button onClick={login}>Login</button>
        </form>
      </div>
    </>
  );
}
