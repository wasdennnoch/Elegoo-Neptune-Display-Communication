package n3p

class SetPageCommand(val page: DisplayPage) : DisplayCommand("page ${page.pageName}")
