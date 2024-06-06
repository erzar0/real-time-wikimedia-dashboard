/**
 * Header Component
 *
 * The `Header` component is a React functional component used to display a header with a title and an optional subtitle.
 *
 * Props:
 * - `title`: A string representing the main title of the header.
 * - `subtitle` (optional): A string representing the optional subtitle of the header.
 **/

import React from "react";
import "./Header.css";

interface HeaderProps {
  title: string;
  subtitle?: string;
}

const Header: React.FC<HeaderProps> = ({ title, subtitle }) => {
  return (
    <header className="header">
      <h1 className="header-title">{title}</h1>
      {subtitle && <h2 className="header-subtitle">{subtitle}</h2>}
    </header>
  );
};

export default Header;
