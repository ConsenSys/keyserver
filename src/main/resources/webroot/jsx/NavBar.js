//NavBar.jsx

var NavBar = React.createClass({
	contextTypes: {
		history: React.PropTypes.object,
	},
	 
	goToRegister: function(){
		this.context.history.replaceState(null,'/register');
	},
	goToLogin: function(){
		this.context.history.replaceState(null,'/login');
	},
	logout: function(){
		this.props.setKeystoreData('');
		this.context.history.replaceState(null,'/');
	},
  
  render: function() {
	var userOptions;
	if(this.props.keystoreData != ''){
		userOptions = (
			<Nav pullRight>
				<NavItem eventKey={1} onClick={this.logout} >Logout {this.props.keystoreData.identifier}</NavItem>
			</Nav>
		);
	}else{
		userOptions = (
			<Nav pullRight>
				<NavItem eventKey={1} onClick={this.goToRegister} >Register</NavItem>
				<NavItem eventKey={2} onClick={this.goToLogin} >Login</NavItem>
			</Nav>
		);
	}
	  
	return (
    <Navbar inverse>
      <Navbar.Header>
        <Navbar.Brand>
          <Link to="/">LightWallet Keyserver</Link>
        </Navbar.Brand>
        <Navbar.Toggle />
      </Navbar.Header>
      <Navbar.Collapse>
        {userOptions}
      </Navbar.Collapse>
    </Navbar>
    );
  }
});