import React from "react";
import "./Footer.css";

interface FooterProps {
  company: string;
  year: number;
}

const Footer: React.FC<FooterProps> = ({ company, year }) => {
  return (
    <footer className="footer">
      <p className="footer-text">
        <span className="copyleft"> &copy; </span> {year} {company}. No rights
        reserved.
      </p>
    </footer>
  );
};

export default Footer;
